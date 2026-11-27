/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.epsilon.lsp.snippets;

import java.util.List;

/**
 * Snippets shared by the rule-based languages, which all have pre and post blocks.
 */
public class ErlSnippets extends EolSnippets {

	@Override
	protected void addSnippets(List<Snippet> snippets) {
		super.addSnippets(snippets);
		snippets.add(new Snippet("pre", "block executed before the rules",
			"pre ${1:name} {\n\t$0\n}"));
		snippets.add(new Snippet("post", "block executed after the rules",
			"post ${1:name} {\n\t$0\n}"));
	}
}
