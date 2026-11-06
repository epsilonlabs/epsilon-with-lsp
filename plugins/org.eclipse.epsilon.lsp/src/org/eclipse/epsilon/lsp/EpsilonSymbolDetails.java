/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.epsilon.lsp;

/**
 * Values of the "detail" field of the document symbols produced by the
 * Epsilon language server. EOL operations are reported as LSP functions or
 * methods (with their signature as detail); all other Epsilon module elements
 * are reported as {@link org.eclipse.lsp4j.SymbolKind#Object objects}, with
 * one of the constants below as detail to specify exactly what they are.
 *
 * As the detail is omitted when it is identical to the name of the symbol
 * (e.g. for "main" or unnamed "pre" blocks), clients should use
 * {@link #of(String, String)} to determine the type of an object symbol.
 */
public final class EpsilonSymbolDetails {

	public static final String IMPORT = "import";
	public static final String MODEL = "model";
	public static final String MAIN = "main";
	public static final String PRE = "pre";
	public static final String POST = "post";
	public static final String CONTEXT = "context";
	public static final String CONSTRAINT = "constraint";
	public static final String CRITIQUE = "critique";
	public static final String RULE = "rule";
	public static final String MARKER = "marker";

	private EpsilonSymbolDetails() {
	}

	/**
	 * Returns the type of an object symbol given its name and detail.
	 */
	public static String of(String name, String detail) {
		return detail != null ? detail : name;
	}
}
