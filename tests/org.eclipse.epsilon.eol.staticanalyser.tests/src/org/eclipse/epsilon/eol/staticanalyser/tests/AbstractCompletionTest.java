package org.eclipse.epsilon.eol.staticanalyser.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.analyse.EolCompletion;
import org.eclipse.epsilon.eol.analyse.EolCompletionKind;
import org.eclipse.epsilon.eol.analyse.EolCompletionParseRepairer;
import org.eclipse.epsilon.eol.analyse.EolStaticAnalyser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractCompletionTest extends AbstractBaseTest {

	private final CompletionExpectationParser completionExpectationParser = new CompletionExpectationParser();
	protected EolModule module;
	protected EolStaticAnalyser staticAnalyser;
	protected List<CompletionExpectation> completionExpectations;
	protected String programSource;

	protected AbstractCompletionTest(String testTag, File epsilonProgramFile, boolean outputToConsole) {
		super(testTag, epsilonProgramFile, outputToConsole);
	}

	@Before
	public void setUp() throws Exception {
		programSource = new String(Files.readAllBytes(programFile.toPath()), StandardCharsets.UTF_8);
		module = new EolModule();
		staticAnalyser = new EolStaticAnalyser(createModelFactory());
		module.parse(programSource, programFile);
		if (module.getParseProblems().isEmpty()) {
			staticAnalyser.validate(module);
		}
		completionExpectations = completionExpectationParser.extractCompletionExpectations(programFile);
	}

	@After
	public void cleanUp() {
		module = null;
		staticAnalyser = null;
		completionExpectations = null;
		programSource = null;
	}

	@Test
	public void completionsMatchExpectations() throws Exception {
		if (isConsoleOutputActive) {
			System.out.println("\nTesting program: " + testTag);
		}
		assertFalse("No completion expectations found in " + testTag, completionExpectations.isEmpty());

		for (CompletionExpectation expectation : completionExpectations) {
			checkCompletionExpectation(expectation);
		}
	}

	protected void checkCompletionExpectation(CompletionExpectation expectation) throws Exception {
		List<EolCompletion> completions = getCompletions(expectation);
		assertCompletionNames(expectation, completions.stream().map(EolCompletion::getName).collect(Collectors.toList()));
		for (Map.Entry<String, List<List<String>>> entry : expectation.getExpectedParameters().entrySet()) {
			List<String> expected = entry.getValue().stream().map(Object::toString).sorted().collect(Collectors.toList());
			List<String> actual = completions.stream()
				.filter(c -> c.getKind() == EolCompletionKind.OPERATION && c.getName().equals(entry.getKey()))
				.map(c -> c.getParameterNames().toString()).sorted().collect(Collectors.toList());
			assertEquals(failureContext(expectation) + "\nParameters for " + entry.getKey(), expected, actual);
		}
	}

	protected void assertCompletionNames(CompletionExpectation expectation, Collection<String> names) {
		Set<String> actualNames = new HashSet<>(names);
		assertEquals(failureContext(expectation)
			+ "\nExpected: " + formatNames(expectation.getExpectedNames())
			+ "\nActual:   " + formatNames(actualNames), expectation.getExpectedNames(), actualNames);
	}

	private List<EolCompletion> getCompletions(CompletionExpectation expectation) throws Exception {
		String repairedSource = repairCompletionSource(expectation);
		if (!repairedSource.equals(programSource) || !module.getParseProblems().isEmpty()) {
			EolModule repairedModule = new EolModule();
			repairedModule.parse(repairedSource, programFile);
			EolStaticAnalyser repairedStaticAnalyser = new EolStaticAnalyser(createModelFactory());
			repairedStaticAnalyser.validate(repairedModule);
			return repairedStaticAnalyser.getCompletions(repairedModule, expectation.getPosition());
		}

		return staticAnalyser.getCompletions(module, expectation.getPosition());
	}

	private String repairCompletionSource(CompletionExpectation expectation) {
		return new EolCompletionParseRepairer().repair(programSource, expectation.getPosition());
	}

	protected String failureContext(CompletionExpectation expectation) {
		return "Completions mismatch for " + testTag
			+ " at [" + expectation.getLine() + ":" + expectation.getColumn() + "]"
			+ "\nSource: " + expectation.getSourceLine().trim();
	}

	private List<String> formatNames(Set<String> names) {
		List<String> sortedNames = new ArrayList<String>(names);
		Collections.sort(sortedNames);
		return sortedNames;
	}
}
