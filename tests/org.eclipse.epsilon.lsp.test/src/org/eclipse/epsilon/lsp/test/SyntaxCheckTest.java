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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SyntaxCheckTest extends AbstractEpsilonLanguageServerTest {

    private static final String GOOD_SYNTAX_EOL_PATH = "../org.eclipse.epsilon.lsp.test/epsilon/01-good-syntax.eol";
    private static final String BAD_SYNTAX_EOL_PATH = "../org.eclipse.epsilon.lsp.test/epsilon/02-bad-syntax.eol";

	@Test
	public void goodEOL() throws Exception {
		final int version = 1;
		final String fileURI = didOpen(new File(GOOD_SYNTAX_EOL_PATH), version);
		assertPublishedEmptyDiagnostics(fileURI);
	}
	
	@Test
	public void badEOL() throws Exception {
		final int version = 1;
		final String fileURI = didOpen(new File(BAD_SYNTAX_EOL_PATH), version);
		List<String> messages = new ArrayList<String>();
		messages.add("no viable alternative at input '('");
		messages.add("no viable alternative at input ')'");
		messages.add("no viable alternative at input '.'");
		assertPublishedExprectedDiagnostics(fileURI, messages);
	}
	
	

}
