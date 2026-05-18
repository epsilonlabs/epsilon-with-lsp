package org.eclipse.epsilon.eol.staticanalyser.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.staticanalyser.EolCompletion;
import org.eclipse.epsilon.eol.staticanalyser.EolCompletionParseRepairer;
import org.eclipse.epsilon.eol.staticanalyser.EolStaticAnalyser;
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
		staticAnalyser = new EolStaticAnalyser(new StaticModelFactory());
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
		assertFalse("No completion expectations found in " + testTag, completionExpectations.isEmpty());

		for (CompletionExpectation expectation : completionExpectations) {
			List<EolCompletion> completions = getCompletions(expectation);
			Set<String> actualNames = new HashSet<String>();
			for (EolCompletion completion : completions) {
				actualNames.add(completion.getName());
			}
			assertEquals(formatFailureMessage(expectation, actualNames), expectation.getExpectedNames(), actualNames);
		}
	}

	private List<EolCompletion> getCompletions(CompletionExpectation expectation) throws Exception {
		if (module.getParseProblems().isEmpty()) {
			return staticAnalyser.getCompletions(module, expectation.getPosition());
		}

		String repairedSource = new EolCompletionParseRepairer().repair(programSource, expectation.getPosition());
		EolModule repairedModule = new EolModule();
		repairedModule.parse(repairedSource, programFile);
		EolStaticAnalyser repairedStaticAnalyser = new EolStaticAnalyser(new StaticModelFactory());
		repairedStaticAnalyser.validate(repairedModule);
		return repairedStaticAnalyser.getCompletions(repairedModule, expectation.getPosition());
	}

	private String formatFailureMessage(CompletionExpectation expectation, Set<String> actualNames) {
		return "Completions mismatch for " + testTag
			+ " at [" + expectation.getLine() + ":" + expectation.getColumn() + "]"
			+ "\nSource: " + expectation.getSourceLine().trim()
			+ "\nExpected: " + formatNames(expectation.getExpectedNames())
			+ "\nActual:   " + formatNames(actualNames);
	}

	private List<String> formatNames(Set<String> names) {
		List<String> sortedNames = new ArrayList<String>(names);
		Collections.sort(sortedNames);
		return sortedNames;
	}
}
