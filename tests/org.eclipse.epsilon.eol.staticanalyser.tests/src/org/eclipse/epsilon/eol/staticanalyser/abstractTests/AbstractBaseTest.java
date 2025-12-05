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
	protected final File epsilonTestFile;
	protected final File epsilonTestFolder;
	
	
	public AbstractBaseTest(String testTag, File epsilonTestFile) {
		this(testTag, epsilonTestFile,false);
	}
	
	public AbstractBaseTest(String testTag, File epsilonTestFile, boolean outputToConsole) {
		this.testTag= testTag;
		this.epsilonTestFile = epsilonTestFile;
		this.epsilonTestFolder = epsilonTestFile.getParentFile();
		this.isConsoleOutputActive = outputToConsole;
	}
	
	
	private static List<File> findEpsilonScriptsWithin(File baseFolder, String fileExtension) {
		List<File> fileList = new ArrayList<File>();
		
		// root folder files	
		File[] rootEolTestFiles = baseFolder.listFiles(fn -> fn.getName().endsWith(fileExtension));
		Arrays.sort(rootEolTestFiles, (a, b) -> a.getName().compareTo(b.getName()));
		for (File file : rootEolTestFiles) {
			fileList.add(file);
		}
				
		// sub folders files
		File[] subdirs = baseFolder.listFiles(f -> f.isDirectory());
		for (File subdir : subdirs) {
			File[] eolTestFiles = subdir.listFiles(fn -> fn.getName().endsWith(fileExtension));
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
	protected static Collection<Object[]> getTestCollection(String resourceFolder, String scriptSetFolder, String fileExtension) throws FileNotFoundException {
		File testFolder = new File(resourceFolder,scriptSetFolder);
		if (!testFolder.exists()) {
			throw new FileNotFoundException("Failed to find the test resources folder: " + testFolder);
		}
		
		List<File> epsilonFiles = AbstractBaseTest.findEpsilonScriptsWithin(testFolder, fileExtension);
		Collection<Object[]> testCollection = new ArrayList<>();
		for (File file : epsilonFiles) {	
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

}
