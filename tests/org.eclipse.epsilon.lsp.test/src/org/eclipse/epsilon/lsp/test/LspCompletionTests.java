package org.eclipse.epsilon.lsp.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.epsilon.lsp.Analyser;
import org.eclipse.epsilon.lsp.EpsilonLanguageServer;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemCapabilities;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.InsertTextMode;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LspCompletionTests extends AbstractEpsilonLanguageServerTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@BeforeClass
	public static void registerUrlHandler() {
		TestUrlHandlerRegistrar.registerSingletonMapHandlerOnce();
	}

	@Before
	@Override
	public void setUp() {
		server = new TestServer();
		testClient = createTestClient();
		server.connect(testClient);
		docService = server.getTextDocumentService();
	}

	@Test
	public void completionServiceForwardsRequestAndItems() throws Exception {
		initialize(null);
		RecordingAnalyser analyser = new RecordingAnalyser(server, cannedCompletions());
		// initialize() replaces the analyser, so install the stub afterwards.
		((TestServer) server).setAnalyser(analyser);
		String uri = "file:///workspace/completion%20forwarding.eol";

		List<CompletionItem> actual = completions(uri, new Position(4, 7));

		assertEquals(URI.create(uri), analyser.uri);
		assertEquals(new Position(4, 7), analyser.position);
		// Use independent items so in-place metadata changes also fail the assertion.
		assertEquals(cannedCompletions(), actual);
	}

	@Test
	public void completionConversionRespectsSnippetSupport() throws Exception {
		File program = temporaryFolder.newFile("completion.eol");
		Files.writeString(program.toPath(), "var food = 1;\nfo;\n"
			+ "operation foo(x : Integer, y : Integer) {}\noperation foo() {}\n", StandardCharsets.UTF_8);

		for (Boolean snippetSupport : new Boolean[] { true, false, null }) {
			initialize(snippetSupport);
			String uri = program.getCanonicalFile().toURI().toString();
			try {
				didOpen(program, 1);
				List<String> actual = completions(uri, new Position(1, 2)).stream()
					.map(this::insertion).sorted().toList();
				List<String> expected = Boolean.TRUE.equals(snippetSupport)
					? List.of("Method PlainText foo()", "Method Snippet foo(${1:x}, ${2:y})$0",
						"Snippet Snippet for (${1:iterator} in ${2:collection}) {\n\t$0\n}",
						"Snippet Snippet for (${1:iterator} in ${2:collection}) {\n\t($0).println();\n}",
						"Variable PlainText food")
					: List.of("Method PlainText foo()", "Method PlainText foo()", "Variable PlainText food");
				assertEquals("snippetSupport=" + snippetSupport, expected, actual);
			}
			finally {
				docService.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));
			}
		}
	}

	@Test
	public void completionsMatchingTheExpectedArgumentTypeAreSortedFirst() throws Exception {
		File program = temporaryFolder.newFile("arguments.eol");
		Files.writeString(program.toPath(), "var count : Integer = 1;\nvar greeting : String = \"hello\";\n"
			+ "\"hello\".concat();\n", StandardCharsets.UTF_8);

		initialize(true);
		String uri = program.getCanonicalFile().toURI().toString();
		try {
			didOpen(program, 1);
			List<String> actual = completions(uri, new Position(2, 15)).stream()
				.sorted(Comparator.comparing(CompletionItem::getSortText))
				.map(CompletionItem::getLabel).toList();
			assertEquals(List.of("greeting", "count"), actual);
		}
		finally {
			docService.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));
		}
	}

	@Test
	public void typesHaveTheClassKind() throws Exception {
		File program = temporaryFolder.newFile("types.eol");
		Files.writeString(program.toPath(), "var x : Integ\n", StandardCharsets.UTF_8);

		initialize(true);
		String uri = program.getCanonicalFile().toURI().toString();
		try {
			didOpen(program, 1);
			CompletionItem integer = completions(uri, new Position(0, 13)).stream()
				.filter(item -> item.getLabel().equals("Integer")).findFirst().orElseThrow();
			assertEquals(CompletionItemKind.Class, integer.getKind());
		}
		finally {
			docService.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));
		}
	}

	@Test
	public void propertiesAreSortedBeforeOperations() throws Exception {
		File program = temporaryFolder.newFile("properties.eol");
		Files.writeString(program.toPath(), "var list : Native(\"java.util.ArrayList\");\nlist.\n",
			StandardCharsets.UTF_8);

		initialize(true);
		String uri = program.getCanonicalFile().toURI().toString();
		try {
			didOpen(program, 1);
			List<CompletionItemKind> kinds = completions(uri, new Position(1, 5)).stream()
				.sorted(Comparator.comparing(CompletionItem::getSortText))
				.map(CompletionItem::getKind)
				.filter(kind -> kind == CompletionItemKind.Property || kind == CompletionItemKind.Method)
				.toList();
			assertTrue(kinds.contains(CompletionItemKind.Property));
			assertTrue(kinds.contains(CompletionItemKind.Method));
			assertTrue(kinds.lastIndexOf(CompletionItemKind.Property) < kinds.indexOf(CompletionItemKind.Method));
		}
		finally {
			docService.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));
		}
	}

	@Test
	public void snippetsAreOfferedOnlyForTheFirstWordOfALine() throws Exception {
		File program = temporaryFolder.newFile("snippets.eol");
		Files.writeString(program.toPath(), "var items = Sequence{};\n\t\nif\nitems.i\nvar x = i\n",
			StandardCharsets.UTF_8);

		initialize(true);
		String uri = program.getCanonicalFile().toURI().toString();
		try {
			didOpen(program, 1);
			List<String> emptyLine = snippetLabels(uri, new Position(1, 1));
			assertEquals(List.of("assert", "assertError", "for", "forp", "function", "if", "if else", "import",
				"operation", "var", "while"), emptyLine);
			assertEquals(List.of("if", "if else"), snippetLabels(uri, new Position(2, 2)));
			assertEquals(List.of(), snippetLabels(uri, new Position(3, 7)));
			assertEquals(List.of(), snippetLabels(uri, new Position(4, 9)));
		}
		finally {
			docService.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));
		}
	}

	@Test
	public void snippetsAdjustIndentation() throws Exception {
		File program = temporaryFolder.newFile("indentation.eol");
		Files.writeString(program.toPath(), "\tfor\n", StandardCharsets.UTF_8);

		initialize(true);
		String uri = program.getCanonicalFile().toURI().toString();
		try {
			didOpen(program, 1);
			CompletionItem forSnippet = completions(uri, new Position(0, 4)).stream()
				.filter(item -> item.getKind() == CompletionItemKind.Snippet && "for".equals(item.getLabel()))
				.findFirst().orElseThrow();
			assertEquals(InsertTextMode.AdjustIndentation, forSnippet.getInsertTextMode());
			assertEquals("iterate over collection", forSnippet.getDetail());
		}
		finally {
			docService.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));
		}
	}

	@Test
	public void evlAndEgxSnippetsIncludeEolSnippets() throws Exception {
		initialize(true);
		assertSnippetLabelsInclude("snippets.evl", List.of("constraint", "context", "fix", "for", "operation"));
		assertSnippetLabelsInclude("snippets.egx", List.of("for", "operation", "post", "pre", "transform"));
	}

	@Test
	public void eglSnippetsDependOnTheSection() throws Exception {
		File template = temporaryFolder.newFile("snippets.egl");
		Files.writeString(template.toPath(), "Hello \n[%\n\n%]\n[%=  %]\n[% gen\n", StandardCharsets.UTF_8);

		initialize(true);
		String uri = template.getCanonicalFile().toURI().toString();
		try {
			didOpen(template, 1);
			assertEquals(List.of("[% %]", "[%= %]", "[* *]", "[*- *]", "for"), snippetLabels(uri, new Position(0, 6)));
			List<String> statement = snippetLabels(uri, new Position(2, 0));
			assertTrue(statement.containsAll(List.of("for", "generate", "if", "process")));
			assertFalse(statement.contains("[% %]"));
			assertEquals(List.of(), snippetLabels(uri, new Position(4, 4)));
			assertEquals(List.of("generate"), snippetLabels(uri, new Position(5, 6)));
		}
		finally {
			docService.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));
		}
	}

	@Test
	public void eglSectionsFollowTheEglLexer() throws Exception {
		File template = temporaryFolder.newFile("sections.egl");
		Files.writeString(template.toPath(), "[* a [% b\n\n*]\n[% var x = 1; -%]\n\n[%\n\n%]\n",
			StandardCharsets.UTF_8);

		initialize(true);
		String uri = template.getCanonicalFile().toURI().toString();
		try {
			didOpen(template, 1);
			// A [% inside a comment does not start a code section
			assertEquals(List.of(), snippetLabels(uri, new Position(1, 0)));
			// -%] ends a code section
			assertEquals(List.of("[% %]", "[%= %]", "[* *]", "[*- *]", "for"), snippetLabels(uri, new Position(4, 0)));
			assertTrue(snippetLabels(uri, new Position(6, 0)).contains("generate"));
		}
		finally {
			docService.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));
		}
	}

	private void assertSnippetLabelsInclude(String fileName, List<String> expected) throws Exception {
		File program = temporaryFolder.newFile(fileName);
		Files.writeString(program.toPath(), "\n", StandardCharsets.UTF_8);
		String uri = program.getCanonicalFile().toURI().toString();
		try {
			didOpen(program, 1);
			List<String> actual = snippetLabels(uri, new Position(0, 0));
			assertTrue(fileName + ": " + actual, actual.containsAll(expected));
		}
		finally {
			docService.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));
		}
	}

	private List<String> snippetLabels(String uri, Position position) throws Exception {
		return completions(uri, position).stream()
			.filter(item -> item.getKind() == CompletionItemKind.Snippet)
			.map(CompletionItem::getLabel).sorted().toList();
	}

	private void initialize(Boolean snippetSupport) throws Exception {
		CompletionItemCapabilities itemCapabilities = new CompletionItemCapabilities();
		itemCapabilities.setSnippetSupport(snippetSupport);
		TextDocumentClientCapabilities textDocument = new TextDocumentClientCapabilities();
		textDocument.setCompletion(new CompletionCapabilities(itemCapabilities));
		ClientCapabilities capabilities = new ClientCapabilities();
		capabilities.setTextDocument(textDocument);
		InitializeParams params = new InitializeParams();
		params.setCapabilities(capabilities);
		initializeResult = server.initialize(params).get(5, TimeUnit.SECONDS);
	}

	private List<CompletionItem> completions(String uri, Position position) throws Exception {
		var response = docService.completion(new CompletionParams(new TextDocumentIdentifier(uri), position))
			.get(5, TimeUnit.SECONDS);
		return response.isLeft() ? response.getLeft() : response.getRight().getItems();
	}

	private String insertion(CompletionItem item) {
		InsertTextFormat format = item.getInsertTextFormat() == null
			? InsertTextFormat.PlainText : item.getInsertTextFormat();
		return item.getKind() + " " + format + " " + item.getInsertText();
	}

	private List<CompletionItem> cannedCompletions() {
		List<CompletionItem> items = new ArrayList<>();
		for (String type : List.of("String", "Integer")) {
			CompletionItem item = new CompletionItem("foo(x : " + type + ")");
			item.setKind(CompletionItemKind.Method);
			item.setDetail(type);
			item.setDocumentation("Overload for " + type);
			item.setFilterText("foo");
			item.setSortText(type);
			item.setInsertText("foo(${1:x})$0");
			item.setInsertTextFormat(InsertTextFormat.Snippet);
			items.add(item);
		}
		return items;
	}

	private static class TestServer extends EpsilonLanguageServer {
		void setAnalyser(Analyser analyser) {
			this.analyser = analyser;
		}
	}

	private static class RecordingAnalyser extends Analyser {
		private final List<CompletionItem> items;
		private URI uri;
		private Position position;

		RecordingAnalyser(EpsilonLanguageServer server, List<CompletionItem> items) {
			super(server);
			this.items = items;
		}

		@Override
		public List<CompletionItem> getCompletions(URI uri, Position position) {
			this.uri = uri;
			this.position = position;
			return items;
		}
	}
}
