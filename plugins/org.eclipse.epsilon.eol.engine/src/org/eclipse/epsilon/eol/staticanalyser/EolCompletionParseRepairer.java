package org.eclipse.epsilon.eol.staticanalyser;

import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.epsilon.common.parse.Position;

public class EolCompletionParseRepairer {

	private static final String PLACEHOLDER = "__epsilon_completion_placeholder";

	public String repair(String code, Position position) {
		if (code == null || position == null) {
			return code;
		}

		int offset = offsetAt(code, position);
		String before = code.substring(0, offset);
		String after = code.substring(offset);

		StringBuilder repaired = new StringBuilder(code.length() + PLACEHOLDER.length() + 8);
		repaired.append(before);
		if (needsPlaceholder(before)) {
			repaired.append(PLACEHOLDER);
		}
		repaired.append(after);
		appendMissingDelimiters(repaired);
		appendMissingSemicolon(repaired);
		return repaired.toString();
	}

	protected int offsetAt(String code, Position position) {
		int line = 1;
		int column = 0;
		for (int i = 0; i < code.length(); i++) {
			if (line == position.getLine() && column == position.getColumn()) {
				return i;
			}
			char c = code.charAt(i);
			if (c == '\n') {
				line++;
				column = 0;
			}
			else {
				column++;
			}
		}
		return code.length();
	}

	protected boolean needsPlaceholder(String beforeCursor) {
		int index = previousNonWhitespaceIndex(beforeCursor);
		if (index < 0) {
			return true;
		}

		char previous = beforeCursor.charAt(index);
		if (previous == '.' || previous == '|' || previous == '(' || previous == '[' || previous == '{'
				|| previous == ',' || previous == ':' || previous == '=' || previous == '?' || previous == '>'
				|| previous == '+' || previous == '-' || previous == '*' || previous == '/') {
			return true;
		}
		return index > 0 && previous == '-' && beforeCursor.charAt(index - 1) == '>';
	}

	protected int previousNonWhitespaceIndex(String text) {
		for (int i = text.length() - 1; i >= 0; i--) {
			if (!Character.isWhitespace(text.charAt(i))) {
				return i;
			}
		}
		return -1;
	}

	protected void appendMissingDelimiters(StringBuilder repaired) {
		Deque<Character> delimiters = new ArrayDeque<Character>();
		for (int i = 0; i < repaired.length(); i++) {
			char c = repaired.charAt(i);
			if (c == '(' || c == '[' || c == '{') {
				delimiters.push(c);
			}
			else if (c == ')' || c == ']' || c == '}') {
				if (!delimiters.isEmpty() && matches(delimiters.peek(), c)) {
					delimiters.pop();
				}
			}
		}

		while (!delimiters.isEmpty()) {
			repaired.append(closingDelimiterFor(delimiters.pop()));
		}
	}

	protected boolean matches(char opening, char closing) {
		return (opening == '(' && closing == ')')
			|| (opening == '[' && closing == ']')
			|| (opening == '{' && closing == '}');
	}

	protected char closingDelimiterFor(char opening) {
		switch (opening) {
			case '(':
				return ')';
			case '[':
				return ']';
			case '{':
				return '}';
			default:
				return ')';
		}
	}

	protected void appendMissingSemicolon(StringBuilder repaired) {
		int index = previousNonWhitespaceIndex(repaired.toString());
		if (index < 0) {
			return;
		}
		char last = repaired.charAt(index);
		if (last != ';' && last != '}') {
			repaired.append(';');
		}
	}
}
