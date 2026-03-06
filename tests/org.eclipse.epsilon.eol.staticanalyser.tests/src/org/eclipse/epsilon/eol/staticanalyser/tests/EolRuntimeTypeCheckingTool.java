package org.eclipse.epsilon.eol.staticanalyser.tests;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.temporal.ValueRange;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import junit.framework.AssertionFailedError;

@RunWith(Parameterized.class)
public class EolRuntimeTypeCheckingTool extends AbstractStaticAnalysisTest {

	private static final String RESOURCES = PROJECT_BASE_FOLDER + "resources/runtimeTypeCheckTool";
	private static final String PROGRAMSET = "gen"; // sub-folder in resources
	private static final String PROGRAMFILEEXTENSION = ".eol";
	private static final String MODELSET = "../models"; // sub-folder in resources
	private static final String MODELFILEEXTENSION = ".ecore";
	private static final boolean ENABLECONSOLEOUTPUT = true;

	@BeforeClass
	public static void registerModelset() {
		registerModels(RESOURCES, MODELSET, MODELFILEEXTENSION);
		
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() throws FileNotFoundException {	
		// Find and execute all the EOL programs that generate the type test EOL programs
		Collection<Object[]> generators = AbstractBaseTest.getEpsilonProgramCollection(RESOURCES, "/src", ".eol");
		for (Object[] object : generators) {
			try {
				EolModule genModule = new EolModule();
				genModule.parse((File)object[1]);
				
				Map<String, Object> config = new HashMap<>();
				config.put("basePath", RESOURCES + "/" + PROGRAMSET + "/");
				config.put("filename", "gen_" + object[0].toString().replaceFirst("/", ""));
				genModule.getContext().getFrameStack().put(
					    Variable.createReadOnlyVariable("config", config) );
				
				genModule.execute();
				if (ENABLECONSOLEOUTPUT) {
					System.out.println("\nExecuted : " + object[1]);
					System.out.println("Config state : " + genModule.getContext().getFrameStack().get("config"));
				}
				genModule = null;
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.err.print("FAILED TO EXECUTE EOL: " + object[1]);
				e.printStackTrace();
				
			}
		}
		
		// Get a list of the generate type test EOL programs, these are the Junit test parameters 
		return getEpsilonProgramCollection(RESOURCES, PROGRAMSET, PROGRAMFILEEXTENSION);
	}

	public EolRuntimeTypeCheckingTool(String testTag, File epsilonTestFile) {
		super(testTag, epsilonTestFile, ENABLECONSOLEOUTPUT);
	}
	
	@Test
	public void emptyFileTest() {
		// Check for empty file
		File file = programFile;
		if (file.exists() && file.isFile()) {
            if (file.length() == 0) {
            	fail("The file is empty.");
            }
        } else {
        	fail("File does not exist or is a directory.");
        }
	}
	
}


