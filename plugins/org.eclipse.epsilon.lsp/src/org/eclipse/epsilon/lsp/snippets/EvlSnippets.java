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

public class EvlSnippets extends EolSnippets {

	@Override
	protected void addSnippets(List<Snippet> snippets) {
		super.addSnippets(snippets);
		snippets.add(new Snippet("context", "context",
			"context ${1:classname} {\n\t$0\n}"));
		snippets.add(new Snippet("constraint", "constraint",
			"constraint ${1:name} {\n\tcheck : $2\n\tmessage : $0\n}"));
		snippets.add(new Snippet("fix", "fix",
			"fix {\n\ttitle : $1\n\tdo {\n\t\t$0\n\t}\n}"));
	}
}
