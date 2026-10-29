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

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.InsertTextMode;
import org.eclipse.lsp4j.Position;

/**
 * Base class for the snippets of a language. Subclasses contribute snippets
 * by overriding {@link #addSnippets(List)}, calling super to inherit the
 * snippets of the language they extend.
 */
public abstract class Snippets {

	private List<Snippet> snippets;

	protected abstract void addSnippets(List<Snippet> snippets);

	protected List<Snippet> getSnippets() {
		if (snippets == null) {
			snippets = new ArrayList<>();
			addSnippets(snippets);
		}
		return snippets;
	}

	/**
	 * Returns the snippets that can be inserted at the given offset of the
	 * code. By default, snippets are only offered for the first word of a line.
	 *
	 * @param textBeforeWord the text on the line before the word being typed
	 */
	protected List<Snippet> getSnippets(String code, int offset, String textBeforeWord) {
		return textBeforeWord.isBlank() ? getSnippets() : Collections.emptyList();
	}

	public List<CompletionItem> getCompletionItems(String code, Position position) {
		int offset = getOffset(code, position);
		if (offset < 0) {
			return Collections.emptyList();
		}
		int wordStart = offset;
		while (wordStart > 0 && Character.isJavaIdentifierPart(code.charAt(wordStart - 1))) {
			wordStart--;
		}
		int lineStart = wordStart;
		while (lineStart > 0 && code.charAt(lineStart - 1) != '\n' && code.charAt(lineStart - 1) != '\r') {
			lineStart--;
		}
		String prefix = code.substring(wordStart, offset);
		String textBeforeWord = code.substring(lineStart, wordStart);

		List<CompletionItem> items = new ArrayList<>();
		for (Snippet snippet : getSnippets(code, offset, textBeforeWord)) {
			if (snippet.getName().startsWith(prefix)) {
				CompletionItem item = new CompletionItem(snippet.getName());
				item.setKind(CompletionItemKind.Snippet);
				item.setDetail(snippet.getDescription());
				item.setInsertText(snippet.getBody());
				item.setInsertTextFormat(InsertTextFormat.Snippet);
				item.setInsertTextMode(InsertTextMode.AdjustIndentation);
				item.setSortText("1" + snippet.getName());
				items.add(item);
			}
		}
		return items;
	}

	/**
	 * Converts an LSP position to an offset in the code, or returns -1 if the
	 * position is outside the code.
	 */
	protected static int getOffset(String code, Position position) {
		if (code == null) {
			return -1;
		}
		int offset = 0;
		for (int line = 0; line < position.getLine(); line++) {
			int lineEnd = code.indexOf('\n', offset);
			if (lineEnd < 0) {
				return -1;
			}
			offset = lineEnd + 1;
		}
		int lineEnd = code.indexOf('\n', offset);
		if (lineEnd < 0) {
			lineEnd = code.length();
		}
		if (lineEnd > offset && code.charAt(lineEnd - 1) == '\r') {
			lineEnd--;
		}
		return Math.min(offset + position.getCharacter(), lineEnd);
	}
}
