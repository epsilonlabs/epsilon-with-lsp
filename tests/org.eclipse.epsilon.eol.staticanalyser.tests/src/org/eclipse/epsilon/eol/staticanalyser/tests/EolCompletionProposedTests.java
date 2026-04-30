package org.eclipse.epsilon.eol.staticanalyser.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class EolCompletionProposedTests extends AbstractCompletionTest {

	private static final String RESOURCES = PROJECT_BASE_FOLDER + "resources/proposed";
	private static final String PROGRAMSET = "completionPrograms";
	private static final String PROGRAMFILEEXTENSION = ".eol";
	private static final boolean ENABLECONSOLEOUTPUT = true;

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() throws FileNotFoundException {
		return getEpsilonProgramCollection(RESOURCES, PROGRAMSET, PROGRAMFILEEXTENSION);
	}

	public EolCompletionProposedTests(String testTag, File epsilonTestFile) {
		super(testTag, epsilonTestFile, ENABLECONSOLEOUTPUT);
	}
}
