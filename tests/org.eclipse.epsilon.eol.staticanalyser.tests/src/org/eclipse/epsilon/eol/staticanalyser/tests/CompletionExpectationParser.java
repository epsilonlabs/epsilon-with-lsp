package org.eclipse.epsilon.eol.staticanalyser.tests;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CompletionExpectationParser {

	private static final String LINE_MARKER = "//@";
	private static final String BLOCK_MARKER_START = "/*@";
	private static final String BLOCK_MARKER_END = "*/";

	public List<CompletionExpectation> extractCompletionExpectations(File testProgram) throws IOException {
		List<CompletionExpectation> expectations = new ArrayList<CompletionExpectation>();
		List<String> lines = Files.readAllLines(testProgram.toPath());

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			int lineMarkerStart = line.indexOf(LINE_MARKER);
			int blockMarkerStart = line.indexOf(BLOCK_MARKER_START);
			if (lineMarkerStart < 0 && blockMarkerStart < 0) {
				continue;
			}

			int markerStart;
			String expectedNamesText;
			if (lineMarkerStart >= 0 && (blockMarkerStart < 0 || lineMarkerStart < blockMarkerStart)) {
				markerStart = lineMarkerStart;
				expectedNamesText = line.substring(markerStart + LINE_MARKER.length()).trim();
			}
			else {
				markerStart = blockMarkerStart;
				int markerEnd = line.indexOf(BLOCK_MARKER_END, markerStart + BLOCK_MARKER_START.length());
				if (markerEnd < 0) {
					fail("Malformed completion expectation on line " + (i + 1) + ": " + line);
				}
				expectedNamesText = line.substring(markerStart + BLOCK_MARKER_START.length(), markerEnd).trim();
			}

			expectations.add(parseExpectation(expectedNamesText, i + 1, markerStart, line));
		}

		return expectations;
	}

	private CompletionExpectation parseExpectation(String text, int lineNumber, int column, String line) {
		Set<String> expectedNames = new LinkedHashSet<String>();
		Map<String, List<List<String>>> expectedParameters = new LinkedHashMap<>();
		if (!text.isEmpty()) {
			for (String part : splitCompletions(text, lineNumber, line)) {
				String token = part.trim();
				int opening = token.indexOf('(');
				String name = opening < 0 ? token : token.substring(0, opening).trim();
				if (name.isEmpty()) {
					fail("Malformed completion expectation on line " + lineNumber + ": " + line);
				}
				if (!expectedNames.add(name) && (opening < 0 || !expectedParameters.containsKey(name))) {
					fail("Duplicate completion name '" + name + "' on line " + lineNumber + ": " + line);
				}
				if (opening >= 0) {
					if (token.indexOf(')', opening) != token.length() - 1) {
						fail("Malformed completion expectation on line " + lineNumber + ": " + line);
					}
					List<String> parameters = new ArrayList<>();
					String arguments = token.substring(opening + 1, token.length() - 1).trim();
					if (!arguments.isEmpty()) {
						for (String argument : arguments.split(",", -1)) {
							if (argument.trim().isEmpty()) {
								fail("Empty parameter name on line " + lineNumber + ": " + line);
							}
							parameters.add(argument.trim());
						}
					}
					expectedParameters.computeIfAbsent(name, key -> new ArrayList<>()).add(parameters);
				}
			}
		}
		return new CompletionExpectation(lineNumber, column, expectedNames, expectedParameters, line);
	}

	private List<String> splitCompletions(String text, int lineNumber, String line) {
		List<String> parts = new ArrayList<>();
		int depth = 0;
		int start = 0;
		for (int i = 0; i < text.length(); i++) {
			char character = text.charAt(i);
			if (character == '(') depth++;
			else if (character == ')') depth--;
			else if (character == ',' && depth == 0) {
				parts.add(text.substring(start, i));
				start = i + 1;
			}
			if (depth < 0 || depth > 1) {
				fail("Malformed completion expectation on line " + lineNumber + ": " + line);
			}
		}
		if (depth != 0) {
			fail("Malformed completion expectation on line " + lineNumber + ": " + line);
		}
		parts.add(text.substring(start));
		return parts;
	}
}
