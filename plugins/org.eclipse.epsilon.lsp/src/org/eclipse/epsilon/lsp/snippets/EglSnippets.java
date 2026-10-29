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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.egl.parse.EglLexer;
import org.eclipse.epsilon.egl.parse.EglRecognitionException;
import org.eclipse.epsilon.egl.parse.EglToken;
import org.eclipse.epsilon.egl.parse.EglToken.TokenType;

/**
 * Inside [% %] sections, offers the EOL snippets plus EGL-specific statements.
 * In template text, offers the section delimiters and a templated for loop.
 */
public class EglSnippets extends EolSnippets {

	private List<Snippet> templateTextSnippets;

	@Override
	protected void addSnippets(List<Snippet> snippets) {
		super.addSnippets(snippets);
		snippets.add(new Snippet("generate", "invokes a subtemplate and stores results in a file",
			"var ${1:templateName} : Template;\n"
			+ "// Pass parameters to the template\n"
			+ "$1 := TemplateFactory.load('$1.egl');\n"
			+ "$1.populate('${2:parameterName}', ${3:parameterValue});\n"
			+ "$1.generate('${4:targetFile}');"));
		snippets.add(new Snippet("process", "invoke a subtemplate and return results",
			"var ${1:templateName} : Template;\n"
			+ "// Pass parameters to the template\n"
			+ "$1 := TemplateFactory.load('$1.egl');\n"
			+ "$1.populate('${2:parameterName}', ${3:parameterValue});\n"
			+ "var ${4:result} := $1.process();"));
	}

	protected void addTemplateTextSnippets(List<Snippet> snippets) {
		snippets.add(new Snippet("[% %]", "dynamic block", "[%$0%]"));
		snippets.add(new Snippet("[%= %]", "output block", "[%=$0%]"));
		snippets.add(new Snippet("[* *]", "multiline comment", "[*$0*]"));
		snippets.add(new Snippet("[*- *]", "outline view marker", "[*-$0*]"));
		snippets.add(new Snippet("for", "iterate over collection",
			"[%for (${1:iterator} in ${2:collection}) { %]\n\t$0\n[%}%]"));
	}

	protected List<Snippet> getTemplateTextSnippets() {
		if (templateTextSnippets == null) {
			templateTextSnippets = new ArrayList<>();
			addTemplateTextSnippets(templateTextSnippets);
		}
		return templateTextSnippets;
	}

	@Override
	protected List<Snippet> getSnippets(String code, int offset, String textBeforeWord) {
		switch (getSection(code, offset)) {
			case PLAIN_TEXT:
				return getTemplateTextSnippets();
			case START_TAG:
				if (textBeforeWord.isBlank() || textBeforeWord.stripTrailing().endsWith("[%")) {
					return getSnippets();
				}
				return Collections.emptyList();
			default:
				// Output sections contain an expression, not statements, and comments contain no code
				return Collections.emptyList();
		}
	}

	/**
	 * Returns the kind of section the offset is in: PLAIN_TEXT for template text,
	 * START_TAG for [% %], START_OUTPUT_TAG for [%= %] and START_COMMENT_TAG for
	 * comments and markers. Sections are recognised by the EGL lexer, following
	 * the same rules as the EGL parser.
	 */
	protected TokenType getSection(String code, int offset) {
		// The lexer counts lines and columns from 1, and only \n terminates a line
		int line = 1;
		int lineStart = 0;
		for (int index = code.indexOf('\n'); index >= 0 && index < offset; index = code.indexOf('\n', index + 1)) {
			line++;
			lineStart = index + 1;
		}
		int column = offset - lineStart + 1;

		TokenType section = TokenType.PLAIN_TEXT;
		EglLexer lexer = new EglLexer(code);
		try {
			for (EglToken token = lexer.nextToken(); token.getTokenType() != TokenType.EOF; token = lexer.nextToken()) {
				if (token.getLine() > line
						|| token.getLine() == line && token.getColumn() + token.getText().length() > column) {
					break;
				}
				section = getSectionAfter(section, token.getTokenType());
			}
		}
		catch (EglRecognitionException e) {
			// Not thrown by the current lexer; treat the remaining code as part of the current section
		}
		return section;
	}

	protected TokenType getSectionAfter(TokenType section, TokenType token) {
		switch (section) {
			case PLAIN_TEXT:
				switch (token) {
					case START_TAG:
					case START_OUTPUT_TAG:
						return token;
					case START_COMMENT_TAG:
					case START_MARKER_TAG:
						return TokenType.START_COMMENT_TAG;
					default:
						return section;
				}
			case START_COMMENT_TAG:
				return token == TokenType.END_COMMENT_TAG ? TokenType.PLAIN_TEXT : section;
			default:
				return token == TokenType.END_TAG || token == TokenType.END_OUTDENT_TAG ? TokenType.PLAIN_TEXT : section;
		}
	}
}
