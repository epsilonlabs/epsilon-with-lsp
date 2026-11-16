/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.epsilon.lsp.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.epsilon.lsp.Analyser;
import org.eclipse.epsilon.lsp.EpsilonLanguageServer;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DidChangeWatchedFilesCapabilities;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesRegistrationOptions;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.FileSystemWatcher;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.RelativePattern;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests that diagnostics are kept up to date when Epsilon programs are
 * created, changed or deleted on disk while the server is running.
 */
public class WatchedFilesTest extends AbstractEpsilonLanguageServerTest {

	private Path workspace;
	private final List<Registration> registrations = new ArrayList<>();

	@BeforeClass
	public static void registerUrlHandler() {
		TestUrlHandlerRegistrar.registerSingletonMapHandlerOnce();
	}

	@Before
	@Override
	public void setUp() throws Exception {
		workspace = Files.createTempDirectory("epsilon-lsp-watched-files").toRealPath();
		server = new EpsilonLanguageServer();
		testClient = new TestClient() {
			@Override
			public CompletableFuture<Void> registerCapability(RegistrationParams params) {
				registrations.addAll(params.getRegistrations());
				return CompletableFuture.completedFuture(null);
			}
		};
		server.connect(testClient);

		DidChangeWatchedFilesCapabilities watchedFiles = new DidChangeWatchedFilesCapabilities(true);
		watchedFiles.setRelativePatternSupport(true);
		WorkspaceClientCapabilities workspaceCapabilities = new WorkspaceClientCapabilities();
		workspaceCapabilities.setDidChangeWatchedFiles(watchedFiles);
		ClientCapabilities capabilities = new ClientCapabilities();
		capabilities.setWorkspace(workspaceCapabilities);
		InitializeParams params = new InitializeParams();
		params.setCapabilities(capabilities);
		params.setWorkspaceFolders(List.of(new WorkspaceFolder(workspace.toUri().toString(), "workspace")));

		server.initialize(params).get(5, TimeUnit.SECONDS);
		server.initialized(new InitializedParams());
		docService = server.getTextDocumentService();
	}

	@After
	public void deleteWorkspace() throws IOException {
		try (Stream<Path> paths = Files.walk(workspace)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
				Files.deleteIfExists(path);
			}
		}
	}

	@Test
	public void watchersAreRegisteredForProgramsInWorkspaceFolders() {
		assertEquals(1, registrations.size());
		Registration registration = registrations.get(0);
		assertEquals(EpsilonLanguageServer.WATCHED_FILES_METHOD, registration.getMethod());

		List<FileSystemWatcher> watchers =
			((DidChangeWatchedFilesRegistrationOptions) registration.getRegisterOptions()).getWatchers();
		assertEquals(Analyser.PROGRAM_EXTENSIONS.size(), watchers.size());
		for (String extension : Analyser.PROGRAM_EXTENSIONS) {
			assertTrue("Missing watcher for " + extension, watchers.stream().anyMatch(watcher -> {
				RelativePattern pattern = watcher.getGlobPattern().getRight();
				return pattern.getPattern().equals("**/*." + extension)
					&& pattern.getBaseUri().getLeft().getUri().equals(workspace.toUri().toString());
			}));
		}
	}

	@Test
	public void watchersAreNotRegisteredWithoutClientSupport() throws Exception {
		List<Registration> otherRegistrations = new ArrayList<>();
		EpsilonLanguageServer otherServer = new EpsilonLanguageServer();
		otherServer.connect(new TestClient() {
			@Override
			public CompletableFuture<Void> registerCapability(RegistrationParams params) {
				otherRegistrations.addAll(params.getRegistrations());
				return CompletableFuture.completedFuture(null);
			}
		});
		otherServer.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);
		otherServer.initialized(new InitializedParams());
		otherServer.shutdown().get();

		assertTrue(otherRegistrations.isEmpty());
	}

	@Test
	public void createdProgramIsAnalysed() throws Exception {
		Path program = write("created.eol", "var a:Integer = 1.5;");
		assertNull(getPublishedDiagnostics(uri(program)));

		notify(program, FileChangeType.Created);
		assertPublishedExprectedDiagnostics(uri(program), List.of("Real may not be assigned to Integer"));
	}

	@Test
	public void programsImportingChangedProgramAreAnalysed() throws Exception {
		Path library = write("lib.eol", "operation foo(){}");
		Path main = write("main.eol", "import 'lib.eol';\nfoo();");
		notify(library, FileChangeType.Created);
		notify(main, FileChangeType.Created);
		assertPublishedEmptyDiagnostics(uri(library));
		assertPublishedEmptyDiagnostics(uri(main));

		write("lib.eol", "operation bar(){}");
		notify(library, FileChangeType.Changed);
		assertPublishedExprectedDiagnostics(uri(main), List.of("Undefined operation foo"));
	}

	@Test
	public void diagnosticsOfDeletedProgramAreCleared() throws Exception {
		Path program = write("deleted.eol", "var a:Integer = 1.5;");
		notify(program, FileChangeType.Created);
		assertFalse(getPublishedDiagnostics(uri(program)).isEmpty());

		Files.delete(program);
		notify(program, FileChangeType.Deleted);
		assertPublishedEmptyDiagnostics(uri(program));
	}

	@Test
	public void programsImportingDeletedProgramAreAnalysed() throws Exception {
		Path library = write("lib.eol", "operation foo(){}");
		Path main = write("main.eol", "import 'lib.eol';\nfoo();");
		notify(library, FileChangeType.Created);
		notify(main, FileChangeType.Created);
		assertPublishedEmptyDiagnostics(uri(main));

		Files.delete(library);
		notify(library, FileChangeType.Deleted);
		assertFalse("The unresolved import should be reported",
			getPublishedDiagnostics(uri(main)).isEmpty());
	}

	@Test
	public void programsOpenInEditorsAreNotReanalysedWhenChangedOnDisk() throws Exception {
		Path program = write("open.eol", "var a:Integer = 42;");
		String openUri = didOpen(program.toFile(), 1);
		assertPublishedEmptyDiagnostics(openUri);
		testClient.resetPublishedDiagnostics();

		// The editor's contents take precedence over the file on disk
		write("open.eol", "var a:Integer = 1.5;");
		notify(program, FileChangeType.Changed);
		assertTrue(testClient.publishedDiagnostics.isEmpty());
	}

	private Path write(String fileName, String contents) throws IOException {
		return Files.writeString(workspace.resolve(fileName), contents, StandardCharsets.UTF_8);
	}

	private void notify(Path file, FileChangeType type) {
		server.getWorkspaceService().didChangeWatchedFiles(
			new DidChangeWatchedFilesParams(List.of(new FileEvent(uri(file), type))));
	}

	private static String uri(Path file) {
		return file.toUri().toString();
	}
}
