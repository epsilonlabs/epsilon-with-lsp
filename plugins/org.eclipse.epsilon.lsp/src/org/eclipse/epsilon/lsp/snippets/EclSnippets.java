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

public class EclSnippets extends ErlSnippets {

	@Override
	protected void addSnippets(List<Snippet> snippets) {
		super.addSnippets(snippets);
		snippets.add(new Snippet("match", "match rule",
			"rule ${1:rulename}\n\tmatch l : ${2:leftmodel}!${3:lefttype}\n"
			+ "\twith r : ${4:rightmodel}!${5:righttype} {\n\tcompare : $0\n}"));
		snippets.add(new Snippet("common match", "match rule",
			"rule ${1:name}\n\tmatch l : Left!$1\n\twith r : Right!$1 {\n\n\tcompare : $0\n}"));
		snippets.add(new Snippet("do", "do block of a match rule",
			"do {\n\t$0\n}"));
	}
}
