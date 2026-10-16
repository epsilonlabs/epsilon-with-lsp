package org.eclipse.epsilon.eol.staticanalyser.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.epsilon.common.parse.Position;

public class CompletionExpectation {

	private final Position position;
	private final Set<String> expectedNames;
	private final Map<String, List<List<String>>> expectedParameters;
	private final String sourceLine;

	public CompletionExpectation(int line, int column, Set<String> expectedNames, String sourceLine) {
		this(line, column, expectedNames, Collections.emptyMap(), sourceLine);
	}

	public CompletionExpectation(int line, int column, Set<String> expectedNames,
			Map<String, List<List<String>>> expectedParameters, String sourceLine) {
		this.position = new Position(line, column);
		this.expectedNames = Collections.unmodifiableSet(new LinkedHashSet<String>(expectedNames));
		Map<String, List<List<String>>> parameters = new LinkedHashMap<>();
		for (Map.Entry<String, List<List<String>>> entry : expectedParameters.entrySet()) {
			List<List<String>> overloads = new ArrayList<>();
			for (List<String> names : entry.getValue()) {
				overloads.add(Collections.unmodifiableList(new ArrayList<>(names)));
			}
			parameters.put(entry.getKey(), Collections.unmodifiableList(overloads));
		}
		this.expectedParameters = Collections.unmodifiableMap(parameters);
		this.sourceLine = sourceLine;
	}

	public Position getPosition() {
		return position;
	}

	public Set<String> getExpectedNames() {
		return expectedNames;
	}

	/** Parameter lists for explicitly annotated operations, retaining overloads. */
	public Map<String, List<List<String>>> getExpectedParameters() {
		return expectedParameters;
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
