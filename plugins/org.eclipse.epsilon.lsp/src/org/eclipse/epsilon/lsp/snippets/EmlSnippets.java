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
 * EML modules can also contain ETL transformation rules.
 */
public class EmlSnippets extends EtlSnippets {

	@Override
	protected void addSnippets(List<Snippet> snippets) {
		super.addSnippets(snippets);
		snippets.add(new Snippet("merge", "merge rule",
			"rule ${1:rulename}\n\tmerge l : ${2:leftmodel}!${3:lefttype}\n"
			+ "\twith r : ${4:rightmodel}!${5:righttype}\n"
			+ "\tinto t : ${6:targetmodel}!${7:targettype} {\n\t$0\n}"));
		snippets.add(new Snippet("common merge", "merge rule",
			"rule ${1:Type}\n\tmerge l : Left!$1\n\twith r : Right!$1\n\tinto t : Target!$1 {\n\t$0\n}"));
	}
}
