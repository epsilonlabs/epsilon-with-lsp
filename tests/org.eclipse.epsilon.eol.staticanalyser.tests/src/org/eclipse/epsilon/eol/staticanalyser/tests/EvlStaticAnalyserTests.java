package org.eclipse.epsilon.eol.staticanalyser.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.epsilon.evl.staticanalyser.EvlStaticAnalyser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class EvlStaticAnalyserTests extends EolStaticAnalyserTests {
	
	private static final File SCRIPT_FOLDER = new File(SOURCE_FOLDER, "org/eclipse/epsilon/eol/staticanalyser/tests/scripts/evl");
	
	public EvlStaticAnalyserTests(String fileName) {
		super(fileName);
	}
	
	@Before
	public void setUp() {
		module = new EvlModule();
		staticAnalyser = new EvlStaticAnalyser(new StaticModelFactory());
	}
	
	@Parameters(name = "{0}")
	public static Collection<String> data() {
		List<String> files = new ArrayList<>();

		for (File file : SCRIPT_FOLDER.listFiles()) {
			if (!file.isDirectory()) {
				files.add(file.getName());
			}
		}
		return files;
	}
	
	@Test
	public void testFileParsing() throws Exception {
		File file = new File(SCRIPT_FOLDER, fileName);
		parseFile(file);
	}
}
