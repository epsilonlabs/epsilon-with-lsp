package org.eclipse.epsilon.eol.staticanalyser.proposedTests;
import org.eclipse.epsilon.eol.staticanalyser.abstractTests.AbstractBaseTest;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ProposedTests extends AbstractBaseTest{

	private static final String RESOURCES = "resources";
	private static final String SCRIPTSET = "proposedTestScripts";
	private static final boolean ENABLECONSOLEOUTPUT = true;

	@Parameters(name = "{0}")
	public static Collection data() throws FileNotFoundException {
		return getTestCollection(RESOURCES, SCRIPTSET);
		
	}
	
	public ProposedTests(String testTag, File eolTestFile) {
		super(testTag, eolTestFile, ENABLECONSOLEOUTPUT);
	}
	
}
