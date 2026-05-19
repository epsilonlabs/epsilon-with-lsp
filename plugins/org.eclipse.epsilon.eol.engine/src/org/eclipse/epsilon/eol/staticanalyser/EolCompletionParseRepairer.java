package org.eclipse.epsilon.eol.staticanalyser;

import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.epsilon.common.parse.Position;

public class EolCompletionParseRepairer {

	static final String PLACEHOLDER = "__epsilon_completion_placeholder";

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
		if (needsInlineSemicolon(before, after)) {
			repaired.append(';');
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

	protected boolean needsInlineSemicolon(String beforeCursor, String afterCursor) {
		int index = previousNonWhitespaceIndex(beforeCursor);
		if (index < 0) {
			return false;
		}

		char previous = beforeCursor.charAt(index);
		return canCompleteStatementAtCursor(previous) && nextSignificantTokenStartsOnLaterLine(afterCursor);
	}

	protected boolean canCompleteStatementAtCursor(char previous) {
		return previous == '.'
			|| previous == ')'
			|| previous == ']'
			|| previous == '\''
			|| previous == '"'
			|| Character.isJavaIdentifierPart(previous);
	}

	protected boolean nextSignificantTokenStartsOnLaterLine(String text) {
		boolean seenNewline = false;
		for (int i = 0; i < text.length();) {
			char c = text.charAt(i);
			if (Character.isWhitespace(c)) {
				seenNewline = seenNewline || isLineBreak(c);
				i++;
				continue;
			}

			if (startsWith(text, i, "/*")) {
				int end = text.indexOf("*/", i + 2);
				if (end < 0) {
					return false;
				}
				seenNewline = seenNewline || containsLineBreak(text, i + 2, end);
				i = end + 2;
				continue;
			}

			if (startsWith(text, i, "//")) {
				int end = nextLineBreak(text, i + 2);
				if (end < 0) {
					return false;
				}
				seenNewline = true;
				i = end;
				continue;
			}

			return seenNewline && canStartFollowingStatement(c);
		}
		return false;
	}

	protected boolean startsWith(String text, int index, String prefix) {
		return text.regionMatches(index, prefix, 0, prefix.length());
	}

	protected int nextLineBreak(String text, int fromIndex) {
		for (int i = fromIndex; i < text.length(); i++) {
			if (isLineBreak(text.charAt(i))) {
				return i;
			}
		}
		return -1;
	}

	protected boolean containsLineBreak(String text, int start, int end) {
		for (int i = start; i < end; i++) {
			if (isLineBreak(text.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	protected boolean isLineBreak(char c) {
		return c == '\n' || c == '\r';
	}

	protected boolean canStartFollowingStatement(char c) {
		switch (c) {
			case ';':
			case ')':
			case ']':
			case '}':
			case '.':
			case ',':
			case '+':
			case '-':
			case '*':
			case '/':
			case '|':
			case '?':
			case ':':
			case '=':
				return false;
			default:
				return true;
		}
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
