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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.epsilon.lsp.MapEntryRegistry;
import org.eclipse.epsilon.lsp.EpsilonLanguageServer;
import org.eclipse.epsilon.lsp.EpsilonWorkspaceService;
import org.eclipse.lsp4e.outline.SymbolsModel;
import org.eclipse.lsp4e.outline.SymbolsModel.DocumentSymbolWithURI;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Assume;
import org.junit.Test;

public class DocumentSymbolTest extends AbstractEpsilonLanguageServerTest {

	@BeforeClass
	public static void registerUrlHandler() {
		TestUrlHandlerRegistrar.registerSingletonMapHandlerOnce();
	}

	@Before
	@Override
	public void setUp() throws Exception {
		MapEntryRegistry.getInstance().clear();
		super.setUp();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		try {
			super.tearDown();
		}
		finally {
			MapEntryRegistry.getInstance().clear();
		}
	}

	@Test
	public void advertisesDocumentSymbolSupport() {
		assertEquals(Boolean.TRUE,
			initializeResult.getCapabilities().getDocumentSymbolProvider().getLeft());
		assertEquals(List.of(EpsilonWorkspaceService.OPEN_DOCUMENT_SYMBOL_COMMAND),
			initializeResult.getCapabilities().getExecuteCommandProvider().getCommands());
	}

	@Test
	public void eolSymbolsUseCurrentBufferAndNestImportedDeclarations() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path library = directory.resolve("library.eol");
		Files.writeString(library, "operation importedOnly() {\n}\n", StandardCharsets.UTF_8);

		Path document = directory.resolve("main.eol");
		Files.writeString(document, "operation diskOnly() {\n}\n", StandardCharsets.UTF_8);
		String openText = "import 'library.eol';\n"
			+ "var x = 1;\n"
			+ "\n"
			+ "operation openOnly() {\n"
			+ "}\n";
		String uri = open(document, "eol", openText);

		List<DocumentSymbol> symbols = documentSymbols(uri);
		assertEquals(List.of("library.eol", "main", "openOnly"), names(symbols));
		assertEquals(SymbolKind.Module, symbols.get(0).getKind());
		assertEquals(SymbolKind.Function, symbols.get(1).getKind());
		assertEquals(new Position(3, 10), symbols.get(2).getSelectionRange().getStart());
		assertEquals(new Position(3, 18), symbols.get(2).getSelectionRange().getEnd());
		assertEquals("()", symbols.get(2).getDetail());
		DocumentSymbol libraryImport = symbols.get(0);
		assertNotNull(libraryImport.getChildren());
		assertEquals(List.of("importedOnly"), names(libraryImport.getChildren()));
		assertEquals(libraryImport.getRange(), libraryImport.getChildren().get(0).getRange());
		assertEquals(libraryImport.getSelectionRange(), libraryImport.getChildren().get(0).getSelectionRange());

		didChange(document.toFile(), 2, "operation changed() {\n}\n");
		symbols = documentSymbols(uri);
		assertTrue(names(symbols).contains("changed"));
		assertFalse(names(symbols).contains("openOnly"));

		docService.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));
		symbols = documentSymbols(uri);
		assertTrue(names(symbols).contains("diskOnly"));
		assertFalse(names(symbols).contains("changed"));
	}

	@Test
	@SuppressWarnings("restriction")
	public void importedOutlinesAreRecursive() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Files.writeString(directory.resolve("leaf.eol"), "operation leaf() {}", StandardCharsets.UTF_8);
		Files.writeString(directory.resolve("library.eol"),
			"import 'leaf.eol';\noperation middle() {}", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.eol");
		String source = "import 'library.eol';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		List<DocumentSymbol> symbols = documentSymbols(open(document, "eol", source));
		assertEquals(List.of("library.eol", "root"), names(symbols));
		DocumentSymbol libraryImport = symbols.get(0);
		assertEquals(List.of("leaf.eol", "middle"), names(libraryImport.getChildren()));
		DocumentSymbol leafImport = libraryImport.getChildren().get(0);
		assertEquals(List.of("leaf"), names(leafImport.getChildren()));
		assertEquals(libraryImport.getRange(), leafImport.getRange());
		assertEquals(libraryImport.getRange(), leafImport.getChildren().get(0).getRange());

		SymbolsModel lsp4eModel = new SymbolsModel();
		lsp4eModel.setUri(document.toUri());
		lsp4eModel.update(symbols.stream()
			.map(symbol -> Either.<SymbolInformation, DocumentSymbol>forRight(symbol))
			.collect(Collectors.toList()));
		DocumentSymbolWithURI wrappedLibrary = (DocumentSymbolWithURI) lsp4eModel.getElements()[0];
		DocumentSymbolWithURI wrappedLeaf = (DocumentSymbolWithURI) lsp4eModel.getChildren(wrappedLibrary)[0];
		DocumentSymbolWithURI wrappedLeafOperation =
			(DocumentSymbolWithURI) lsp4eModel.getChildren(wrappedLeaf)[0];
		assertEquals("leaf.eol", wrappedLeaf.symbol.getName());
		assertEquals("leaf", wrappedLeafOperation.symbol.getName());
	}

	@Test
	public void recursiveImportedOutlinesUseNestedOpenBuffers() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path leaf = directory.resolve("leaf.eol");
		Files.writeString(leaf, "operation diskLeaf() {}", StandardCharsets.UTF_8);
		open(leaf, "eol", "operation bufferLeaf() {}");
		Files.writeString(directory.resolve("library.eol"),
			"import 'leaf.eol';\noperation middle() {}", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.eol");
		String source = "import 'library.eol';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		DocumentSymbol libraryImport = documentSymbols(open(document, "eol", source)).get(0);
		DocumentSymbol leafImport = libraryImport.getChildren().get(0);
		assertEquals("leaf.eol", leafImport.getName());
		assertEquals(List.of("bufferLeaf"), names(leafImport.getChildren()));
	}

	@Test
	public void recursiveImportedOutlinesRetainMalformedDescendantImport() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Files.writeString(directory.resolve("leaf.eol"),
			"operation broken() {", StandardCharsets.UTF_8);
		Files.writeString(directory.resolve("library.eol"),
			"import 'leaf.eol';\noperation middle() {}", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.eol");
		String source = "import 'library.eol';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		DocumentSymbol libraryImport = documentSymbols(open(document, "eol", source)).get(0);
		assertEquals(List.of("leaf.eol", "middle"), names(libraryImport.getChildren()));
	}

	@Test
	public void recursiveImportedOutlinesRetainRuntimeReproduction() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Files.writeString(directory.resolve("script4.eol"),
			"model M driver EMF {nsuri=\"http://www.eclipse.org/emf/2002/Ecore\"};\n"
				+ "operation String foo(x : Integer) {}", StandardCharsets.UTF_8);
		Files.writeString(directory.resolve("script3.eol"),
			"import 'script4.eol';\n"
				+ "model X driver Unknow;\n"
				+ "model Y driver EMF {nsuri=\"http://www.eclipse.org/emf/2002/Ecore\"};\n"
				+ "task.all;\n",
			StandardCharsets.UTF_8);
		Path document = directory.resolve("script1.eol");
		String source = "import 'script3.eol';\n"
			+ "model M driver EMF {nsuri=\"http://www.eclipse.org/emf/2002/Ecore\"};\n"
			+ "operation String foo(x : Integer) {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		DocumentSymbol script3Import = documentSymbols(open(document, "eol", source)).get(0);
		assertEquals("script4.eol", script3Import.getChildren().get(0).getName());
		assertEquals(List.of("M", "foo"), names(script3Import.getChildren().get(0).getChildren()));
	}

	@Test
	public void importedSymbolNavigationOpensItsRealLocation() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path leaf = directory.resolve("leaf.eol");
		Files.writeString(leaf, "operation leaf() {}", StandardCharsets.UTF_8);
		Files.writeString(directory.resolve("library.eol"),
			"import 'leaf.eol';\noperation middle() {}", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.eol");
		String source = "import 'library.eol';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		String uri = open(document, "eol", source);
		DocumentSymbol libraryImport = documentSymbols(uri).get(0);
		Location location = navigate(uri, libraryImport, List.of(0, 0));

		assertNotNull(location);
		assertEquals(leaf.toFile().getCanonicalFile().toURI(), URI.create(location.getUri()));
		assertEquals(new Position(0, 10), location.getRange().getStart());
		assertEquals(new Position(0, 14), location.getRange().getEnd());
	}

	@Test
	public void importedSymbolNavigationUsesChildIndexForDuplicateSymbols() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path library = directory.resolve("library.eol");
		Files.writeString(library,
			"operation duplicate() {}\noperation duplicate() {}", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.eol");
		String source = "import 'library.eol';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		String uri = open(document, "eol", source);
		DocumentSymbol libraryImport = documentSymbols(uri).get(0);
		assertEquals(List.of("duplicate", "duplicate"), names(libraryImport.getChildren()));
		Location location = navigate(uri, libraryImport, List.of(1));

		assertNotNull(location);
		assertEquals(new Position(1, 10), location.getRange().getStart());
		assertEquals(new Position(1, 19), location.getRange().getEnd());
	}

	@Test
	public void staleDuplicateSymbolNavigationDoesNotSelectAnotherDuplicate() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path library = directory.resolve("library.eol");
		Files.writeString(library,
			"operation duplicate() {}\noperation duplicate() {}", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.eol");
		String source = "import 'library.eol';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		String uri = open(document, "eol", source);
		DocumentSymbol libraryImport = documentSymbols(uri).get(0);
		didChange(library.toFile(), 2,
			"operation inserted() {}\noperation duplicate() {}\noperation duplicate() {}");

		assertNull(navigate(uri, libraryImport, List.of(1)));
	}

	@Test
	public void staleOutOfBoundsIndexFallsBackToUniqueSymbol() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path library = directory.resolve("library.eol");
		Files.writeString(library,
			"operation removed() {}\noperation retained() {}", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.eol");
		String source = "import 'library.eol';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		String uri = open(document, "eol", source);
		DocumentSymbol libraryImport = documentSymbols(uri).get(0);
		didChange(library.toFile(), 2, "operation retained() {}");

		Location location = navigate(uri, libraryImport, List.of(1));
		assertNotNull(location);
		assertEquals(new Position(0, 10), location.getRange().getStart());
		assertEquals(new Position(0, 18), location.getRange().getEnd());
	}

	@Test
	public void importedSymbolNavigationUsesCurrentImportedBuffer() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path library = directory.resolve("library.eol");
		Files.writeString(library, "operation imported() {}", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.eol");
		String source = "import 'library.eol';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		String uri = open(document, "eol", source);
		DocumentSymbol libraryImport = documentSymbols(uri).get(0);
		didChange(library.toFile(), 2, "operation added() {}\noperation imported() {}");

		Location location = navigate(uri, libraryImport, List.of(0));

		assertNotNull(location);
		assertEquals(library.toFile().getCanonicalFile().toURI(), URI.create(location.getUri()));
		assertEquals(new Position(1, 10), location.getRange().getStart());
		assertEquals(new Position(1, 18), location.getRange().getEnd());
	}

	@Test
	public void importedSymbolNavigationRejectsHashCollidingReplacement() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path library = directory.resolve("library.eol");
		Files.writeString(library, "operation Aa() {}", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.eol");
		String source = "import 'library.eol';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		String uri = open(document, "eol", source);
		DocumentSymbol libraryImport = documentSymbols(uri).get(0);
		didChange(library.toFile(), 2, "operation BB() {}");

		assertNull(navigate(uri, libraryImport, List.of(0)));
	}

	@Test
	public void latestImportedSymbolNavigationRequestWins() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path library = directory.resolve("library.eol");
		Files.writeString(library,
			"operation first() {}\noperation second() {}", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.eol");
		String source = "import 'library.eol';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		String uri = open(document, "eol", source);
		DocumentSymbol libraryImport = documentSymbols(uri).get(0);
		var first = EpsilonWorkspaceService.createOpenDocumentSymbolCommand(
			URI.create(uri), libraryImport, List.of(0));
		var second = EpsilonWorkspaceService.createOpenDocumentSymbolCommand(
			URI.create(uri), libraryImport, List.of(1));

		Location secondLocation = executeNavigation(second);
		assertNotNull(secondLocation);
		assertEquals(new Position(1, 10), secondLocation.getRange().getStart());
		assertNull(executeNavigation(first));
	}

	@Test
	public void absoluteImportNavigationUsesCurrentImportedBuffer() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path library = directory.resolve("library.eol");
		Files.writeString(library, "operation imported() {}", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.eol");
		String source = "import '" + library.toAbsolutePath().toString().replace('\\', '/')
			+ "';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		String uri = open(document, "eol", source);
		DocumentSymbol libraryImport = documentSymbols(uri).get(0);
		didChange(library.toFile(), 2, "operation added() {}\noperation imported() {}");
		assertEquals(List.of("added", "imported"), names(documentSymbols(uri).get(0).getChildren()));

		Location location = navigate(uri, libraryImport, List.of(0));

		assertNotNull(location);
		assertEquals(new Position(1, 10), location.getRange().getStart());
	}

	@Test
	public void absoluteImportUsesValidBufferWhenDiskSourceIsMalformed() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path library = directory.resolve("library.eol");
		Files.writeString(library, "operation broken() {", StandardCharsets.UTF_8);
		open(library, "eol", "operation imported() {}");
		Path document = directory.resolve("main.eol");
		String source = "import '" + library.toAbsolutePath().toString().replace('\\', '/')
			+ "';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		String uri = open(document, "eol", source);
		DocumentSymbol libraryImport = documentSymbols(uri).get(0);
		assertEquals(List.of("imported"), names(libraryImport.getChildren()));

		Location location = navigate(uri, libraryImport, List.of(0));

		assertNotNull(location);
		assertEquals(new Position(0, 10), location.getRange().getStart());
	}

	@Test
	public void absoluteImportThroughSymlinkUsesCanonicalBufferAndLocation() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols").toRealPath();
		Path alias = directory.resolveSibling(directory.getFileName() + "-alias");
		try {
			Files.createSymbolicLink(alias, directory);
		}
		catch (IOException | UnsupportedOperationException | SecurityException ex) {
			Assume.assumeNoException(ex);
			return;
		}

		Path library = directory.resolve("library.eol");
		Files.writeString(library, "operation broken() {", StandardCharsets.UTF_8);
		MapEntryRegistry.getInstance().putCode(
			library.toFile().getCanonicalFile().toURI().getPath(), "operation imported() {}");
		Path document = directory.resolve("main.eol");
		String source = "import '" + alias.resolve("library.eol").toString().replace('\\', '/')
			+ "';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		String uri = open(document, "eol", source);
		DocumentSymbol libraryImport = documentSymbols(uri).get(0);
		assertEquals(List.of("imported"), names(libraryImport.getChildren()));
		Location location = navigate(uri, libraryImport, List.of(0));
		assertEquals(library.toFile().getCanonicalFile().toURI(), URI.create(location.getUri()));

		didChange(library.toFile(), 2, "operation added() {}\noperation imported() {}");
		assertEquals(List.of("added", "imported"), names(documentSymbols(uri).get(0).getChildren()));
	}

	@Test
	public void importedOutlineCyclesTerminateAtTheBackReference() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path document = directory.resolve("main.eol");
		String source = "import 'library.eol';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);
		Files.writeString(directory.resolve("library.eol"),
			"import 'main.eol';\noperation imported() {}", StandardCharsets.UTF_8);

		List<DocumentSymbol> symbols = documentSymbols(open(document, "eol", source));
		DocumentSymbol libraryImport = symbols.get(0);
		assertEquals(List.of("main.eol", "imported"), names(libraryImport.getChildren()));
		assertNull(libraryImport.getChildren().get(0).getChildren());
	}

	@Test
	public void unopenedOutlineCyclesTerminateAcrossMapEntryAndFileUris() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path document = directory.resolve("main.eol");
		Path library = directory.resolve("library.eol");
		String source = "import 'library.eol';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);
		Files.writeString(library,
			"import '" + document.toAbsolutePath().toString().replace('\\', '/')
				+ "';\noperation imported() {}", StandardCharsets.UTF_8);

		DocumentSymbol libraryImport = documentSymbols(document.toUri().toString()).get(0);
		assertEquals(List.of(document.toAbsolutePath().toString().replace('\\', '/'), "imported"),
			names(libraryImport.getChildren()));
		assertNull(libraryImport.getChildren().get(0).getChildren());
	}

	@Test
	public void duplicateImportsHaveIndependentOutlines() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Files.writeString(directory.resolve("library.eol"),
			"operation imported() {}", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.eol");
		String source = "import 'library.eol';\nimport 'library.eol';\noperation root() {}";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		List<DocumentSymbol> symbols = documentSymbols(open(document, "eol", source));
		assertEquals(List.of("library.eol", "library.eol", "root"), names(symbols));
		assertEquals(List.of("imported"), names(symbols.get(0).getChildren()));
		assertEquals(List.of("imported"), names(symbols.get(1).getChildren()));
		assertFalse(symbols.get(0).getChildren() == symbols.get(1).getChildren());
	}

	@Test
	public void malformedSiblingRangesRemainValidAndDisjoint() throws Exception {
		String source = "model M driver EMF {nsuri='x'}\n"
			+ "var x = 1;\n";
		String uri = createWithCachedContents("malformed.eol", source);

		List<DocumentSymbol> symbols = documentSymbols(uri);
		assertEquals(List.of("M", "main"), names(symbols));
		assertTrue(compare(symbols.get(0).getRange().getEnd(), symbols.get(1).getRange().getStart()) <= 0);
		for (DocumentSymbol symbol : symbols) {
			assertPositionWithinSource(symbol.getRange().getStart(), source);
			assertPositionWithinSource(symbol.getRange().getEnd(), source);
			assertPositionWithinSource(symbol.getSelectionRange().getStart(), source);
			assertPositionWithinSource(symbol.getSelectionRange().getEnd(), source);
		}
	}

	@Test
	public void evlConstraintContextsHaveNestedSymbols() throws Exception {
		String source = "constraint GlobalConstraint {\n"
			+ "  check : true\n"
			+ "}\n"
			+ "\n"
			+ "context Person {\n"
			+ "  constraint HasName {\n"
			+ "    check : true\n"
			+ "  }\n"
			+ "  critique NeedsAge {\n"
			+ "    check : true\n"
			+ "  }\n"
			+ "}\n";
		String uri = createWithCachedContents("outline.evl", source);

		List<DocumentSymbol> symbols = documentSymbols(uri);
		assertEquals(List.of("GlobalConstraint", "Person"), names(symbols));
		assertEquals("constraint", symbols.get(0).getDetail());

		DocumentSymbol context = symbols.get(1);
		assertEquals(SymbolKind.Class, context.getKind());
		assertEquals(new Position(4, 8), context.getSelectionRange().getStart());
		assertEquals(new Position(4, 14), context.getSelectionRange().getEnd());
		assertEquals(List.of("HasName", "NeedsAge"), names(context.getChildren()));
		assertEquals("critique", context.getChildren().get(1).getDetail());
	}

	@Test
	public void eglMarkersAndOperationsAreSymbols() throws Exception {
		String source = "[*- Heading *]\n"
			+ "[%\n"
			+ "operation render() {\n"
			+ "}\n"
			+ "%]\n";
		String uri = createAndOpen("outline.egl", "egl", source);

		List<DocumentSymbol> symbols = documentSymbols(uri);
		assertEquals(List.of("Heading", "render"), names(symbols));
		assertEquals(SymbolKind.String, symbols.get(0).getKind());
		assertEquals(new Position(0, 0), symbols.get(0).getRange().getStart());
		assertEquals(new Position(0, 14), symbols.get(0).getRange().getEnd());
		assertEquals(new Position(2, 10), symbols.get(1).getSelectionRange().getStart());
	}

	@Test
	public void importedEglOutlinesUseTemplateDeclarations() throws Exception {
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Files.writeString(directory.resolve("library.egl"),
			"[*- Library *]\n[% operation child() {} %]", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.egl");
		String source = "[% import 'library.egl'; %]\n[*- Main *]";
		Files.writeString(document, source, StandardCharsets.UTF_8);

		List<DocumentSymbol> symbols = documentSymbols(open(document, "egl", source));
		assertEquals(List.of("library.egl", "Main"), names(symbols));
		assertEquals(List.of("Library", "child"), names(symbols.get(0).getChildren()));
		assertFalse(names(symbols.get(0).getChildren()).contains("main"));
	}

	@Test
	public void egxRulesAndLifecycleBlocksAreSymbols() throws Exception {
		String source = "pre {\n"
			+ "}\n"
			+ "rule Generate {\n"
			+ "  template : 'template.egl'\n"
			+ "}\n"
			+ "post {\n"
			+ "}\n"
			+ "operation helper() {\n"
			+ "}\n";
		String uri = createAndOpen("outline.egx", "egx", source);

		List<DocumentSymbol> symbols = documentSymbols(uri);
		assertEquals(List.of("pre", "Generate", "post", "helper"), names(symbols));
		assertEquals(List.of(SymbolKind.Event, SymbolKind.Function, SymbolKind.Event, SymbolKind.Function),
			symbols.stream().map(DocumentSymbol::getKind).collect(Collectors.toList()));
	}

	@Test
	public void unsupportedDocumentsReturnNoSymbols() throws Exception {
		assertTrue(documentSymbols("file:///tmp/outline.txt").isEmpty());
	}

	@Test
	public void nonHierarchicalClientsReceiveFlatSymbolInformation() throws Exception {
		EpsilonLanguageServer flatServer = new EpsilonLanguageServer();
		flatServer.connect(createTestClient());
		flatServer.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);
		String source = "pre {\n"
			+ "}\n"
			+ "context Person {\n"
			+ "  constraint HasName {\n"
			+ "    check : true\n"
			+ "  }\n"
			+ "}\n";
		String uri = createWithCachedContents("flat.evl", source);

		try {
			List<Either<SymbolInformation, DocumentSymbol>> response = documentSymbolResponse(
				flatServer.getTextDocumentService(), uri);
			assertTrue(response.stream().allMatch(Either::isLeft));
			assertEquals(List.of("pre", "Person", "HasName"), response.stream()
				.map(symbol -> symbol.getLeft().getName()).collect(Collectors.toList()));
			assertEquals(SymbolKind.Function, response.get(0).getLeft().getKind());
			assertNull(response.get(1).getLeft().getContainerName());
			assertEquals("Person", response.get(2).getLeft().getContainerName());
		}
		finally {
			flatServer.shutdown().get(5, TimeUnit.SECONDS);
		}
	}

	@Test
	public void nonHierarchicalImportedSymbolsUseTheirRealLocations() throws Exception {
		EpsilonLanguageServer flatServer = new EpsilonLanguageServer();
		flatServer.connect(createTestClient());
		flatServer.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);
		Path directory = Files.createTempDirectory("epsilon-lsp-document-symbols");
		Path library = directory.resolve("library.eol");
		Files.writeString(library, "operation imported() {}", StandardCharsets.UTF_8);
		Path document = directory.resolve("main.eol");
		Files.writeString(document, "import 'library.eol';\noperation root() {}", StandardCharsets.UTF_8);

		try {
			List<Either<SymbolInformation, DocumentSymbol>> response = documentSymbolResponse(
				flatServer.getTextDocumentService(), document.toUri().toString());
			assertEquals(List.of("library.eol", "imported", "root"), response.stream()
				.map(symbol -> symbol.getLeft().getName()).collect(Collectors.toList()));
			assertEquals(library.toFile().getCanonicalFile().toURI(),
				URI.create(response.get(1).getLeft().getLocation().getUri()));
			assertEquals(new Position(0, 10), response.get(1).getLeft().getLocation().getRange().getStart());
		}
		finally {
			flatServer.shutdown().get(5, TimeUnit.SECONDS);
		}
	}

	@Test
	public void shutdownClearsOpenBufferContents() throws Exception {
		Path document = Files.createTempFile("epsilon-lsp-document-symbols", ".eol");
		Files.writeString(document, "operation diskOnly() {}", StandardCharsets.UTF_8);
		String unsaved = "operation unsaved() {}";
		String uri = open(document, "eol", unsaved);
		String path = URI.create(uri).getPath();
		assertEquals(unsaved, MapEntryRegistry.getInstance().getCode(path));

		server.shutdown().get(5, TimeUnit.SECONDS);
		assertNull(MapEntryRegistry.getInstance().getCode(path));
	}

	private String createAndOpen(String fileName, String languageId, String source) throws Exception {
		Path document = Files.createTempDirectory("epsilon-lsp-document-symbols").resolve(fileName);
		Files.writeString(document, source, StandardCharsets.UTF_8);
		return open(document, languageId, source);
	}

	private String createWithCachedContents(String fileName, String source) throws Exception {
		Path document = Files.createTempDirectory("epsilon-lsp-document-symbols").resolve(fileName);
		Files.writeString(document, source, StandardCharsets.UTF_8);
		MapEntryRegistry.getInstance().putCode(document.toUri().getPath(), source);
		return document.toUri().toString();
	}

	private String open(Path document, String languageId, String source) throws Exception {
		String uri = document.toFile().getCanonicalFile().toURI().toString();
		docService.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(uri, languageId, 1, source)));
		return uri;
	}

	private Location navigate(String uri, DocumentSymbol root, List<Integer> childPath) throws Exception {
		return executeNavigation(EpsilonWorkspaceService.createOpenDocumentSymbolCommand(
			URI.create(uri), root, childPath));
	}

	private Location executeNavigation(ExecuteCommandParams command) throws Exception {
		Object result = server.getWorkspaceService().executeCommand(command).get(5, TimeUnit.SECONDS);
		return EpsilonWorkspaceService.toDocumentSymbolLocation(result);
	}

	private List<DocumentSymbol> documentSymbols(String uri) throws Exception {
		List<Either<SymbolInformation, DocumentSymbol>> response = documentSymbolResponse(uri);
		assertTrue(response.stream().allMatch(Either::isRight));
		return response.stream().map(Either::getRight).collect(Collectors.toList());
	}

	private List<Either<SymbolInformation, DocumentSymbol>> documentSymbolResponse(String uri) throws Exception {
		return documentSymbolResponse(docService, uri);
	}

	private List<Either<SymbolInformation, DocumentSymbol>> documentSymbolResponse(
			TextDocumentService service, String uri) throws Exception {
		DocumentSymbolParams params = new DocumentSymbolParams(new TextDocumentIdentifier(uri));
		return service.documentSymbol(params).get(5, TimeUnit.SECONDS);
	}

	private List<String> names(List<DocumentSymbol> symbols) {
		return symbols.stream().map(DocumentSymbol::getName).collect(Collectors.toList());
	}

	private int compare(Position first, Position second) {
		int lineComparison = Integer.compare(first.getLine(), second.getLine());
		return lineComparison != 0 ? lineComparison : Integer.compare(first.getCharacter(), second.getCharacter());
	}

	private void assertPositionWithinSource(Position position, String source) {
		List<String> lines = List.of(source.split("\\n", -1));
		assertTrue(position.getLine() >= 0 && position.getLine() < lines.size());
		assertTrue(position.getCharacter() >= 0
			&& position.getCharacter() <= lines.get(position.getLine()).length());
	}
}
