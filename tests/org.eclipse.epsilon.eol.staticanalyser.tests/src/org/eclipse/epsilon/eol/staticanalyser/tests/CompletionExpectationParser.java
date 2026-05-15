package org.eclipse.epsilon.eol.staticanalyser.tests;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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

			Set<String> expectedNames = parseExpectedNames(expectedNamesText, i + 1, line);
			expectations.add(new CompletionExpectation(i + 1, markerStart, expectedNames, line));
		}

		return expectations;
	}

	private Set<String> parseExpectedNames(String expectedNamesText, int lineNumber, String line) {
		Set<String> expectedNames = new LinkedHashSet<String>();
		if (expectedNamesText.isEmpty()) {
			return expectedNames;
		}

		String[] parts = expectedNamesText.split(",");
		for (String part : parts) {
			String expectedName = part.trim();
			if (expectedName.isEmpty()) {
				fail("Malformed completion expectation on line " + lineNumber + ": " + line);
			}

			if (!expectedNames.add(expectedName)) {
				fail("Duplicate completion name '" + expectedName + "' on line " + lineNumber + ": " + line);
			}
		}

		return expectedNames;
	}
}
