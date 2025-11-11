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

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.epsilon.lsp.EpsilonLanguageServer;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.Before;
import org.junit.Test;

public class SyntaxCheckTest extends AbstractEpsilonLanguageServerTest {

    private static final String GOOD_SYNTAX_EOL_PATH = "../org.eclipse.epsilon.lsp.test/epsilon/01-good-syntax.eol";
    private static final String BAD_SYNTAX_EOL_PATH = "../org.eclipse.epsilon.lsp.test/epsilon/02-bad-syntax.eol";
    private static final String STATIC_ANALYSIS_ERROR_EOL_PATH = "../org.eclipse.epsilon.lsp.test/epsilon/03-static-analysis-error.eol";
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

}
