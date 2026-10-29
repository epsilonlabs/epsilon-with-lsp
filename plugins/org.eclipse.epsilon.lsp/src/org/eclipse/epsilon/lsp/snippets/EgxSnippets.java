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

public class EgxSnippets extends EolSnippets {

	@Override
	protected void addSnippets(List<Snippet> snippets) {
		super.addSnippets(snippets);
		snippets.add(new Snippet("transform", "transform rule",
			"rule ${1:rulename}\n\ttransform s : ${2:sourcemodel}!${3:sourcetype} {\n\n"
			+ "\ttemplate : \"$4\"\n\n\tparameters : Map{}\n\n\ttarget : \"$0\"\n}"));
		snippets.add(new Snippet("pre", "block executed before the rules",
			"pre ${1:name} {\n\t$0\n}"));
		snippets.add(new Snippet("post", "block executed after the rules",
			"post ${1:name} {\n\t$0\n}"));
	}
}
