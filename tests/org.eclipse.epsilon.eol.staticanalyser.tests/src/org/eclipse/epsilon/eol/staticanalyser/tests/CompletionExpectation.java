package org.eclipse.epsilon.eol.staticanalyser.tests;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.epsilon.common.parse.Position;

public class CompletionExpectation {

	private final Position position;
	private final Set<String> expectedNames;
	private final String sourceLine;

	public CompletionExpectation(int line, int column, Set<String> expectedNames, String sourceLine) {
		this.position = new Position(line, column);
		this.expectedNames = Collections.unmodifiableSet(new LinkedHashSet<String>(expectedNames));
		this.sourceLine = sourceLine;
	}

	public Position getPosition() {
		return position;
	}

	public Set<String> getExpectedNames() {
		return expectedNames;
	}

	public String getSourceLine() {
		return sourceLine;
	}

	public int getLine() {
		return position.getLine();
	}

	public int getColumn() {
		return position.getColumn();
	}

	@Override
	public String toString() {
		return "[" + getLine() + ":" + getColumn() + "] " + expectedNames;
	}
}
