package org.eclipse.epsilon.eol.staticanalyser.tests;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.epsilon.common.parse.Position;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.analyse.EolCompletion;
import org.eclipse.epsilon.eol.analyse.EolCompletionParseRepairer;
import org.eclipse.epsilon.eol.analyse.EolStaticAnalyser;
import org.junit.Test;

/**
 * Checks which completions inside the parentheses of an operation call are
 * marked as matching the type of the parameter at the cursor (written as |).
 */
public class ExpectedArgumentTypeRankingTests {

	private static final String VARIABLES = "var greeting : String = \"hello\";\n"
		+ "var count : Integer = 1;\n"
		+ "var items : Sequence(String) = Sequence{};\n"
		+ "var untyped;\n";

	@Test
	public void variablesAndOperationsOfTheParameterTypeAreRanked() throws Exception {
		assertEquals(Arrays.asList("greeting", "title"), rankedCompletions(VARIABLES
			+ "\"hello\".concat(|);\n"
			+ "operation title() : String { return \"\"; }\n"
			+ "operation total() : Integer { return 1; }\n"));
	}

	@Test
	public void everyOverloadContributesItsParameterType() throws Exception {
		assertEquals(Arrays.asList("count", "greeting"), rankedCompletions(VARIABLES + "\"hello\".indexOf(|);"));
	}

	@Test
	public void laterArgumentsUseTheirOwnParameterType() throws Exception {
		assertEquals(Arrays.asList("count"), rankedCompletions(VARIABLES + "\"hello\".substring(1, |);"));
	}

	@Test
	public void collectionTypeNamesStartingAMatchingLiteralAreRanked() throws Exception {
		assertEquals(Arrays.asList("Bag", "Collection", "List", "OrderedSet", "Sequence", "Set", "items"),
			rankedCompletions(VARIABLES + "items.includesAll(|);"));
	}

	@Test
	public void typeNamesAreRankedForTypeParameters() throws Exception {
		assertEquals(Arrays.asList("Any", "Bag", "Boolean", "Collection", "ConcurrentBag", "ConcurrentMap",
			"ConcurrentSet", "Integer", "List", "Map", "Native", "None", "Nothing", "OrderedSet", "Real", "Sequence",
			"Set", "String", "Tuple"), rankedCompletions(VARIABLES + "greeting.isKindOf(|);"));
	}

	@Test
	public void nothingIsRankedForParametersOfAnyType() throws Exception {
		assertEquals(Collections.emptyList(), rankedCompletions(VARIABLES + "greeting.println(|);"));
	}

	@Test
	public void nothingIsRankedWhenTheArgumentIsMoreThanTheCompletedName() throws Exception {
		assertEquals(Collections.emptyList(), rankedCompletions(VARIABLES + "\"hello\".concat(1 + |);"));
	}

	private List<String> rankedCompletions(String source) throws Exception {
		int offset = source.indexOf('|');
		String code = source.substring(0, offset) + source.substring(offset + 1);
		String beforeCursor = code.substring(0, offset);
		Position position = new Position(beforeCursor.split("\n", -1).length,
			offset - (beforeCursor.lastIndexOf('\n') + 1));

		EolModule module = new EolModule();
		module.parse(new EolCompletionParseRepairer().repair(code, position));
		EolStaticAnalyser staticAnalyser = new EolStaticAnalyser();
		staticAnalyser.validate(module);
		return staticAnalyser.getCompletions(module, position).stream()
			.filter(EolCompletion::isExpectedTypeMatch)
			.map(EolCompletion::getName)
			.sorted()
			.collect(Collectors.toList());
	}
}
