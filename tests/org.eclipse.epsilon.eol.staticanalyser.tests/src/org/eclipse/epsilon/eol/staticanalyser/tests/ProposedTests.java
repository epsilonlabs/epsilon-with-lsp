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
	private static final String SCRIPTGROUP = "proposedTestScripts";

	private static final File TEST_FOLDER = new File(RESOURCES, SCRIPTGROUP);
	
	@Parameters(name = "{0}")
	public static Collection data() {
		List<File> eolFiles = AbstractBaseTest.findEOLScriptsWithin(TEST_FOLDER);
		Collection<Object[]> testData = new ArrayList<>();
		for (File file : eolFiles) {	
			String longTestTag = String.format("%s%s/%s", 
					SCRIPTGROUP,
					file.getParent().replace(TEST_FOLDER.getPath(),""),
					file.getName());
			String shortTestTag = String.format("%s/%s",
					file.getParent().replace(TEST_FOLDER.getPath(),""),
					file.getName());
            testData.add(new Object[] {
            	shortTestTag,	   
                file
            });
        }
		return testData;
		
	}
	
	public ProposedTests(String testTag, File eolTestFile) {
		super(testTag, eolTestFile, false);
		System.out.println("Go ProposedTests");
	}
	


	
}
