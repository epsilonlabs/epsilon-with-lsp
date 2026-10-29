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

public class EolSnippets extends Snippets {

	@Override
	protected void addSnippets(List<Snippet> snippets) {
		snippets.add(new Snippet("function", "function with return type",
			"function ${1:context} ${2:name}() : ${3:returntype} {\n\t$0\n}"));
		snippets.add(new Snippet("operation", "operation with return type",
			"operation ${1:context} ${2:name}() : ${3:returntype} {\n\t$0\n}"));
		snippets.add(new Snippet("for", "iterate over collection",
			"for (${1:iterator} in ${2:collection}) {\n\t$0\n}"));
		snippets.add(new Snippet("forp", "iterate over collection and print something",
			"for (${1:iterator} in ${2:collection}) {\n\t($0).println();\n}"));
		snippets.add(new Snippet("while", "while loop with condition",
			"while (${1:condition}) {\n\t$0\n}"));
		snippets.add(new Snippet("if", "if branch",
			"if (${1:condition}) {\n\t$0\n}"));
		snippets.add(new Snippet("if else", "if branch with else part",
			"if (${1:condition}) {\n\t$2\n}\nelse {\n\t$0\n}"));
		snippets.add(new Snippet("assert", "assertion",
			"assert(${1:condition}, ${2:message});"));
		snippets.add(new Snippet("assertError", "assertion of error",
			"assertError(${1:expression}, ${2:message});"));
		snippets.add(new Snippet("import", "import",
			"import \"${1:filename}\";"));
		snippets.add(new Snippet("var", "variable declaration",
			"var ${1:name} : ${2:type} = ${3:initial_value};"));
	}
}
