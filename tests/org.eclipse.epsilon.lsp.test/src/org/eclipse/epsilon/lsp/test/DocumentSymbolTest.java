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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.epsilon.lsp.MapEntryRegistry;
import org.eclipse.epsilon.lsp.EpsilonLanguageServer;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.InitializeParams;
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
	}

	@Test
	public void eolSymbolsUseCurrentBufferAndExcludeImportedDeclarations() throws Exception {
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

	private String open(Path document, String languageId, String source) {
		String uri = document.toAbsolutePath().toUri().toString();
		docService.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(uri, languageId, 1, source)));
		return uri;
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
