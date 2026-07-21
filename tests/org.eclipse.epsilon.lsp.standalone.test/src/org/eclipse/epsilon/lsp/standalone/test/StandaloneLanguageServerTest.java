package org.eclipse.epsilon.lsp.standalone.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.epsilon.lsp.test.AbstractEpsilonLanguageServerTest;
import org.eclipse.epsilon.lsp.standalone.StandaloneEpsilonLanguageServer;
import org.eclipse.epsilon.lsp.standalone.StandaloneLanguageServerLauncher;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.junit.Before;
import org.junit.Test;

public class StandaloneLanguageServerTest extends AbstractEpsilonLanguageServerTest {

	@Before
	@Override
	public void setUp() throws Exception {
		server = new StandaloneEpsilonLanguageServer();
		testClient = createTestClient();
		server.connect(testClient);
		server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);
		docService = server.getTextDocumentService();
	}

	@Test
	public void workspaceEmfaticFilesAreAddedAndRemovedFromGlobalRegistry() throws Exception {
		String namespaceUri = "urn:epsilon:lsp:test:standalone";
		Path workspace = Files.createTempDirectory("epsilon-lsp-emfatic");
		Path metamodel = workspace.resolve("model.emf");
		Files.write(metamodel, List.of(
			"@namespace(uri=\"" + namespaceUri + "\", prefix=\"standalone\")",
			"package standalone;",
			"class Person {}"), StandardCharsets.UTF_8);

		InitializeParams initializeParams = new InitializeParams();
		initializeParams.setWorkspaceFolders(List.of(
			new WorkspaceFolder(workspace.toUri().toString(), "test")));
		server.initialize(initializeParams).get(5, TimeUnit.SECONDS);

		try {
			assertNotNull(EPackage.Registry.INSTANCE.getEPackage(namespaceUri));

			FileEvent deleted = new FileEvent();
			deleted.setUri(metamodel.toUri().toString());
			deleted.setType(FileChangeType.Deleted);
			server.getWorkspaceService().didChangeWatchedFiles(
				new DidChangeWatchedFilesParams(List.of(deleted)));
			assertNull(EPackage.Registry.INSTANCE.getEPackage(namespaceUri));
		}
		finally {
			EPackage.Registry.INSTANCE.remove(namespaceUri);
		}
	}

	@Test
	public void emfaticDocumentsPublishParserDiagnostics() throws Exception {
		Path metamodel = Files.createTempFile("epsilon-lsp-invalid", ".emf");
		String uri = metamodel.toUri().toString();
		DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(
			new TextDocumentItem(uri, "emfatic", 1, "package ;"));
		docService.didOpen(params);

		for (int i = 0; i < 50 && getPublishedDiagnostics(uri) == null; i++) {
			Thread.sleep(20);
		}

		assertNotNull(getPublishedDiagnostics(uri));
		assertFalse(getPublishedDiagnostics(uri).isEmpty());
	}

	@Test
	public void tcpLauncherServesLanguageServerRequests() throws Exception {
		StandaloneLanguageServerLauncher serverLauncher = new StandaloneLanguageServerLauncher(
			(StandaloneEpsilonLanguageServer) server, "localhost", 0);
		Thread serverThread = new Thread(serverLauncher, "epsilon-lsp-test-server");
		serverThread.start();
		assertTrue(serverLauncher.isStarted().get(5, TimeUnit.SECONDS));

		try (Socket socket = new Socket("localhost", serverLauncher.getPort())) {
			Launcher<LanguageServer> clientLauncher = LSPLauncher.createClientLauncher(
				testClient, socket.getInputStream(), socket.getOutputStream());
			Future<Void> listener = clientLauncher.startListening();
			try {
				InitializeResult result = clientLauncher.getRemoteProxy()
					.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);
				assertNotNull(result);
				clientLauncher.getRemoteProxy().shutdown().get(5, TimeUnit.SECONDS);
			}
			finally {
				listener.cancel(true);
			}
		}
		finally {
			serverLauncher.shutdown();
			serverThread.join(5000);
		}
	}
}
