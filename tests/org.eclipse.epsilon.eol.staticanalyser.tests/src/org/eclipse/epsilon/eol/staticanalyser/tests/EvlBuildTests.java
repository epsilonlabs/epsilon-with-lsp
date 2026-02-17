package org.eclipse.epsilon.eol.staticanalyser.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class EvlBuildTests extends AbstractStaticAnalysisTest {
	
	private static final String RESOURCES = PROJECT_BASE_FOLDER + "resources";
	private static final String PROGRAMSET = "evlPrograms"; // sub-folder in resources
	private static final String PROGRAMFILEEXTENSION = ".evl";
	private static final String MODELSET = "models"; // sub-folder in resources
	private static final String MODELFILEEXTENSION = ".ecore";
	private static final boolean ENABLECONSOLEOUTPUT = true;
		
	@BeforeClass
	public static void registerModelset() {
		registerModels(RESOURCES, MODELSET, MODELFILEEXTENSION);
	}
	
	@Parameters(name = "{0}")
	public static Collection<Object[]> data() throws FileNotFoundException {			
		return getEpsilonProgramCollection(RESOURCES, PROGRAMSET, PROGRAMFILEEXTENSION);
	}
	
	public EvlBuildTests(String testTag, File epsilonTestFile) {
		super(testTag, epsilonTestFile, ENABLECONSOLEOUTPUT);
	}
}
