/*******************************************************************************
 * Copyright (c) 2024 The University of York.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.epsilon.lsp.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.epsilon.common.module.ModuleElement;
import org.eclipse.epsilon.common.parse.AST;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.dom.ForStatement;
import org.eclipse.epsilon.lsp.Analyser;
import org.eclipse.epsilon.lsp.EpsilonLanguageServer;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.Before;
import org.junit.Test;

public class SyntaxCheckTest extends AbstractEpsilonLanguageServerTest {

    private static final String GOOD_SYNTAX_EOL_PATH = "../org.eclipse.epsilon.lsp.test/epsilon/01-good-syntax.eol";
    private static final String BAD_SYNTAX_EOL_PATH = "../org.eclipse.epsilon.lsp.test/epsilon/02-bad-syntax.eol";
    private static final String STATIC_ANALYSIS_ERROR_EOL_PATH = "../org.eclipse.epsilon.lsp.test/epsilon/03-static-analysis-error.eol";
    private static final String INVALID_FOR_ITERATOR_EOL_PATH = "../org.eclipse.epsilon.lsp.test/epsilon/04-invalid-for-iterator.eol";
    private static final String SIMULATED_PARSE_EXCEPTION_EOL_PATH = "../org.eclipse.epsilon.lsp.test/epsilon/05-simulated-parse-exception.eol";
    private static final String FOLDER = "../org.eclipse.epsilon.lsp.test/epsilon/";

    @Before
    @Override
	public void setUp() throws Exception {
		server = new EpsilonLanguageServer();
		testClient = new TestClient();
		server.connect(testClient);

		WorkspaceFolder workspaceFolder = new WorkspaceFolder();
		workspaceFolder.setUri(new File(FOLDER).getAbsoluteFile().toURI().toString());
		InitializeParams ip = new InitializeParams();
		ip.setWorkspaceFolders(List.of(workspaceFolder));
		
		InitializeResult initResults = server.initialize(ip).get(5, TimeUnit.SECONDS);
		assertNotNull("Initialisation should have completed in 5s", initResults);
		assertEquals("Should be using full-text synchronisation",
			TextDocumentSyncKind.Full, initResults.getCapabilities().getTextDocumentSync().getLeft());
		assertTrue("LSP spec: no diagnostics should be published before responding to initialize",
			testClient.publishedDiagnostics.isEmpty());

		// The workspace folders are analysed once the client confirms initialisation
		server.initialized(new InitializedParams());

		docService = server.getTextDocumentService();
    }
    
	@Test
	public void goodEOL() throws Exception {
		final String fileURI = Paths.get(GOOD_SYNTAX_EOL_PATH).toAbsolutePath().toUri().toString();
		assertPublishedEmptyDiagnostics(fileURI);
	}
	
	@Test
	public void badEOL() throws Exception {
		final String fileURI = Paths.get(BAD_SYNTAX_EOL_PATH).toAbsolutePath().toUri().toString();
		List<String> messages = new ArrayList<String>();
		messages.add("no viable alternative at input '('");
		messages.add("no viable alternative at input ')'");
		messages.add("no viable alternative at input '.'");
		assertPublishedExprectedDiagnostics(fileURI, messages);
	}
	
	@Test
	public void staticAnalysisError() throws Exception {
		final String fileURI = Paths.get(STATIC_ANALYSIS_ERROR_EOL_PATH).toAbsolutePath().toUri().toString();
		List<String> messages = new ArrayList<String>();
		messages.add("String cannot be assigned to Integer");
		assertPublishedExprectedDiagnostics(fileURI, messages);
	}

	@Test
	public void invalidForIterator() throws Exception {
		// Parsing this file throws while building the AST of the recovered for loop, but
		// its syntax errors are still reported (without the resulting internal exception)
		final String fileURI = Paths.get(INVALID_FOR_ITERATOR_EOL_PATH).toAbsolutePath().toUri().toString();
		List<String> messages = new ArrayList<String>();
		messages.add("no viable alternative at input 'c'");
		messages.add("no viable alternative at input '-'");
		messages.add("no viable alternative at input 'x'");
		messages.add("no viable alternative at input ':'");
		messages.add("mismatched input '1' expecting NAME");
		assertPublishedExprectedDiagnostics(fileURI, messages);

		Diagnostic forIteratorDiagnostic = getPublishedDiagnostics(fileURI).stream()
			.filter(d -> "mismatched input '1' expecting NAME".equals(d.getMessage().getLeft()))
			.findFirst().orElseThrow();
		assertEquals(new Range(new Position(2, 5), new Position(2, 5)), forIteratorDiagnostic.getRange());
	}

	@Test
	public void exceptionIsReportedWhenParsingThrowsWithoutSyntaxErrors() throws Exception {
		final URI fileURI = Paths.get(SIMULATED_PARSE_EXCEPTION_EOL_PATH).toAbsolutePath().toUri();

		// Discard the diagnostics published when the workspace was analysed
		testClient.resetPublishedDiagnostics();

		// The program is valid, so simulate a failure while building the AST of its for loop
		Analyser analyser = new Analyser(server) {
			@Override
			protected IEolModule createModule(String languageId) {
				return new EolModule() {
					@Override
					public ModuleElement adapt(AST cst, ModuleElement parentAst) {
						ModuleElement element = super.adapt(cst, parentAst);
						if (element instanceof ForStatement) {
							throw new IllegalStateException("Simulated failure");
						}
						return element;
					}
				};
			}
		};
		analyser.processDocument(fileURI);

		assertPublishedExprectedDiagnostics(fileURI.toString(), List.of("Exception during parsing: Simulated failure"));

		// The exception has no location, so it is reported at the start of the document
		assertEquals(new Range(new Position(0, 0), new Position(0, 0)),
			getPublishedDiagnostics(fileURI.toString()).get(0).getRange());
	}

}
