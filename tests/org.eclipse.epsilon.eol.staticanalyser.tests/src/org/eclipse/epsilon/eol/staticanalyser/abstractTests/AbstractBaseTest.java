package org.eclipse.epsilon.eol.staticanalyser.abstractTests;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;


public abstract class AbstractBaseTest {
		
	private final boolean isConsoleOutputActive;	
	protected final String testTag;
	protected final File programFile;
	protected final File programFolder;
	
	protected AbstractBaseTest(String testTag, File epsilonProgramFile) {
		this(testTag, epsilonProgramFile, false);
	}
	
	protected AbstractBaseTest(String testTag, File epsilonProgramFile, boolean outputToConsole) {
		this.testTag= testTag;
		this.programFile = epsilonProgramFile;
		this.programFolder = epsilonProgramFile.getParentFile();
		this.isConsoleOutputActive = outputToConsole;
	}
	
	private static List<File> findFilesWithin(File baseFolder, String fileExtension) {
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
	 * @param epsilonProgramFolder name of a folder in the resource folder containing test scripts
	 * @return Collection EOL scripts as parameter Arrays [String testTag, File file]
	 * @throws FileNotFoundException 
	 */
	protected static Collection<Object[]> getEpsilonProgramCollection(String resourceFolder, String epsilonProgramFolder, String fileExtension) throws FileNotFoundException {
		File testFolder = new File(resourceFolder,epsilonProgramFolder);
		if (!testFolder.exists()) {
			throw new FileNotFoundException("Failed to find the test resources folder: " + testFolder);
		}
		
		List<File> epsilonFiles = AbstractBaseTest.findFilesWithin(testFolder, fileExtension);
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

	private static void registerPackage(String path) {
		// Create a ResourceSet and register the default resource factory
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());

		// Load the .ecore file
		URI fileURI = URI.createFileURI(path);
		Resource resource = resourceSet.getResource(fileURI, true);

		// Extract the EPackage from the loaded resource
		EObject eObject = resource.getContents().get(0);
		if (eObject instanceof EPackage) {
			EPackage ePackage = (EPackage) eObject;

			// Register the EPackage
			EPackage.Registry.INSTANCE.put(ePackage.getNsURI(), ePackage);
		}
	}
	
	protected static void registerModels(String resource, String modelSet, String modelFileExtension) {
		System.out.println("registerEcore models");
		File modelSetFolder = new File(resource, modelSet);
		List<File> modelFileList = AbstractBaseTest.findFilesWithin(modelSetFolder, modelFileExtension);
		
		EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
		for (File modelFile : modelFileList) {
			registerPackage(modelFile.getPath());
			System.out.println(" - path: " + modelFile.getPath());
		}
		
	}


}
