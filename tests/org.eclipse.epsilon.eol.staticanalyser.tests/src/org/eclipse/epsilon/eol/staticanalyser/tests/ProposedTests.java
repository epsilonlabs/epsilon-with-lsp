package org.eclipse.epsilon.eol.staticanalyser.tests;



import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class ProposedTests extends AbstractBaseTest{

	private static final String RESOURCES = "resources";
	private static final String SCRIPTSET = "proposedTestScripts";
	private static final boolean ENABLECONSOLEOUTPUT = true;

	@Parameters(name = "{0}")
	public static Collection data() {
		return getTestCollection(RESOURCES, SCRIPTSET);
		
	}
	
	public ProposedTests(String testTag, File eolTestFile) {
		super(testTag, eolTestFile, ENABLECONSOLEOUTPUT);
	}
	
}
