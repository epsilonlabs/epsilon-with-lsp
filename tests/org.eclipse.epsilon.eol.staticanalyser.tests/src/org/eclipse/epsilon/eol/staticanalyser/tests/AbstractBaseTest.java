package org.eclipse.epsilon.eol.staticanalyser.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;


public abstract class AbstractBaseTest {
	
	private final boolean isConsoleOutputActive;
	
	protected final File eolTestFile;
	protected final File eolTestFolder;
	
	public AbstractBaseTest(String testTag, File eolTestFile) {
		this(testTag, eolTestFile,false);
	}
	
	public AbstractBaseTest(String testTag, File eolTestFile, boolean outputToConsole) {
		System.out.println("AbstractBaseTest(File eolTestFile, boolean outputToConsole)");
		this.eolTestFile = eolTestFile;
		this.eolTestFolder = eolTestFile.getParentFile();
		this.isConsoleOutputActive = outputToConsole;
	}
	
	
	public static List<File> findEOLScriptsWithin(File baseFolder) {
		List<File> fileList = new ArrayList<File>();
		
		// root folder		
		File[] rootEolTestFiles = baseFolder.listFiles(fn -> fn.getName().endsWith(".eol"));
		Arrays.sort(rootEolTestFiles, (a, b) -> a.getName().compareTo(b.getName()));
		for (File file : rootEolTestFiles) {
			System.out.println(" - " + file);
			fileList.add(file);
		}
				
		// sub folders
		File[] subdirs = baseFolder.listFiles(f -> f.isDirectory());
		System.out.println("baseFolder " + baseFolder);
		for (File subdir : subdirs) {
			File[] eolTestFiles = subdir.listFiles(fn -> fn.getName().endsWith(".eol"));
			System.out.println("eolTestFiles " + eolTestFiles.length);
			Arrays.sort(eolTestFiles, (a, b) -> a.getName().compareTo(b.getName()));
			for (File file : eolTestFiles) {
				System.out.println(" - " + file);
				fileList.add(file);
			}
		}
		return fileList;
	}
	
	@Test
	public void test () {
		System.out.println("Test Go");
		System.out.println("\ntesting EOL: " + eolTestFolder + eolTestFile);
		assertTrue(true);
	}
	
		
}
