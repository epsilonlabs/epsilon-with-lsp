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

public class FlockSnippets extends ErlSnippets {

	@Override
	protected void addSnippets(List<Snippet> snippets) {
		super.addSnippets(snippets);
		snippets.add(new Snippet("migrate", "migrate rule",
			"migrate ${1:originaltype} {\n\t$0\n}"));
	}
}
