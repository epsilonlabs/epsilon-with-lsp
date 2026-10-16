package org.eclipse.epsilon.eol.staticanalyser.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CompletionExpectationParserTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void parsesParametersAndOverloadsAtTheMarkerPosition() throws Exception {
		CompletionExpectation expectation = parse("fo/*@foo(x, y), foo(), food*/;");
		assertEquals(1, expectation.getLine());
		assertEquals(2, expectation.getColumn());
		assertEquals(new LinkedHashSet<>(Arrays.asList("foo", "food")), expectation.getExpectedNames());
		assertEquals(Arrays.asList(Arrays.asList("x", "y"), Collections.emptyList()),
			expectation.getExpectedParameters().get("foo"));
	}

	@Test
	public void preservesBareNamesAndEmptyExpectations() throws Exception {
		CompletionExpectation expectation = parse("fo//@foo, food");
		assertEquals(new LinkedHashSet<>(Arrays.asList("foo", "food")), expectation.getExpectedNames());
		assertTrue(expectation.getExpectedParameters().isEmpty());
		assertTrue(parse("fo/*@ */").getExpectedNames().isEmpty());
	}

	@Test
	public void preservesRepeatedParameterListsForOverloads() throws Exception {
		assertEquals(Arrays.asList(Arrays.asList("x", "y"), Arrays.asList("x", "y")),
			parse("fo//@foo(x,y), foo(x, y)").getExpectedParameters().get("foo"));
	}

	@Test
	public void rejectsMalformedExpectations() {
		for (String marker : Arrays.asList("foo(x", "foo(x))", "foo((x))", "foo(x)bar(y)",
				"foo(x,)", "foo,", "foo,foo", "foo,foo(x)", "foo(x),foo")) {
			assertThrows(marker, AssertionError.class, () -> parse("fo/*@" + marker + "*/"));
		}
	}

	private CompletionExpectation parse(String source) throws Exception {
		File file = temporaryFolder.newFile();
		Files.write(file.toPath(), source.getBytes(StandardCharsets.UTF_8));
		return new CompletionExpectationParser().extractCompletionExpectations(file).get(0);
	}
}
