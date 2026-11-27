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
 * Also used for EMG, which has the same syntax as EPL.
 */
public class EplSnippets extends ErlSnippets {

	@Override
	protected void addSnippets(List<Snippet> snippets) {
		super.addSnippets(snippets);
		snippets.add(new Snippet("pattern", "pattern",
			"pattern ${1:rulename}\n\ts : ${2:sourcetype} {\n\tmatch : $0\n}"));
	}
}
