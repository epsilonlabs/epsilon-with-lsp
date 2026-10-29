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

/**
 * A code template. The body uses LSP snippet syntax: ${n:name} placeholders
 * (repeated numbers are edited together) and $0 for the final cursor position.
 */
public class Snippet {

	protected final String name;
	protected final String description;
	protected final String body;

	public Snippet(String name, String description, String body) {
		this.name = name;
		this.description = description;
		this.body = body;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getBody() {
		return body;
	}
}
