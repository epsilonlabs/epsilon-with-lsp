package org.eclipse.epsilon.eol.staticanalyser.abstractTests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;


public abstract class AbstractBaseTest {
	
	private final boolean isConsoleOutputActive;
	
	protected final String testTag;
	protected final File eolTestFile;
	protected final File eolTestFolder;
	
	public AbstractBaseTest(String testTag, File eolTestFile) {
		this(testTag, eolTestFile,false);
	}
	
	public AbstractBaseTest(String testTag, File eolTestFile, boolean outputToConsole) {
		this.testTag= testTag;
		this.eolTestFile = eolTestFile;
		this.eolTestFolder = eolTestFile.getParentFile();
		this.isConsoleOutputActive = outputToConsole;
	}
	
	
	public static List<File> findEOLScriptsWithin(File baseFolder) {
		List<File> fileList = new ArrayList<File>();
		
		// root folder files	
		File[] rootEolTestFiles = baseFolder.listFiles(fn -> fn.getName().endsWith(".eol"));
		Arrays.sort(rootEolTestFiles, (a, b) -> a.getName().compareTo(b.getName()));
		for (File file : rootEolTestFiles) {
			fileList.add(file);
		}
				
		// sub folders files
		File[] subdirs = baseFolder.listFiles(f -> f.isDirectory());
		for (File subdir : subdirs) {
			File[] eolTestFiles = subdir.listFiles(fn -> fn.getName().endsWith(".eol"));
			Arrays.sort(eolTestFiles, (a, b) -> a.getName().compareTo(b.getName()));
			for (File file : eolTestFiles) {
				fileList.add(file);
			}
		}
		return fileList;
	}
	
	/**
	 * @param resourceFolder name of the resource folder containing set of scripts for tests
	 * @param scriptSetFolder name of a folder in the resource folder containing test scripts
	 * @return Collection EOL scripts as parameter Arrays [String testTag, File file]
	 * @throws FileNotFoundException 
	 */
	protected static Collection<Object[]> getTestCollection(String resourceFolder, String scriptSetFolder) throws FileNotFoundException {
		File testFolder = new File(resourceFolder,scriptSetFolder);
		if (!testFolder.exists()) {
			throw new FileNotFoundException("Failed to find the test resources folder: " + testFolder);
		}
		
		List<File> eolFiles = AbstractBaseTest.findEOLScriptsWithin(testFolder);
		Collection<Object[]> testCollection = new ArrayList<>();
		for (File file : eolFiles) {	
			/* I prefer the short tag style, it does not clog the Junit results window
			String longTestTag = String.format("%s%s/%s", 
					scriptSetFolder,
					file.getParent().replace(testFolder.getPath(),""),
					file.getName());
			*/
			String shortTestTag = String.format("%s/%s",
					file.getParent().replace(testFolder.getPath(),""),
					file.getName());
            testCollection.add(new Object[] {
            	shortTestTag,	   
                file
            });
        }
		return testCollection;
	}
	
	@Test
	public void test () {
		System.out.println("\nTesting EOL: " + testTag + "\n - path: "  + eolTestFolder + eolTestFile);
		assertTrue(true);
	}
	
		
}
