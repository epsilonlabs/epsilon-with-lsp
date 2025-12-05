package org.eclipse.epsilon.eol.staticanalyser.abstractTests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.staticanalyser.EolStaticAnalyser;
import org.junit.Test;

public class AbstractEOLTest extends AbstractBaseTest{
		
	protected EolModule module;
	protected EolStaticAnalyser staticAnalyser;
	
	public AbstractEOLTest(String testTag, File epsilonTestFile, boolean enableconsoleoutput) {
		super(testTag, epsilonTestFile, enableconsoleoutput);
	}
	
	@Test
	public void test () {
		System.out.println("\nEOL Testing: " + testTag + "\n - path: "  + programFolder + programFile);
		assertTrue(true);
	}
	
}
