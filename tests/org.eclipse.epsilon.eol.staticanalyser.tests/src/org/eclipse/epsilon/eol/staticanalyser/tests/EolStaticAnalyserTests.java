package org.eclipse.epsilon.eol.staticanalyser.tests;

import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runner.RunWith;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.common.module.ModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.staticanalyser.EolStaticAnalyser;
import org.eclipse.epsilon.eol.staticanalyser.StaticModelFactory;
import org.eclipse.epsilon.eol.types.EolType;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class EolStaticAnalyserTests {

	private String fileName;

	public EolStaticAnalyserTests(String fileName) {
		this.fileName = fileName;
	}

	@BeforeClass
	public static void registerEcore() {
		EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
		registerPackage("src/org/eclipse/epsilon/eol/staticanalyser/tests/meta1.ecore");
	}

	public static void registerPackage(String path) {
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

	@Parameters(name = "{0}")
	public static Collection<String> data() {
		File folder = new File("src/org/eclipse/epsilon/eol/staticanalyser/tests/scripts");
		List<String> files = new ArrayList<>();
		for (File file : folder.listFiles()) {
			if (!file.isDirectory()) {
				files.add(file.getName());
			}
		}
		return files;
	}

	@Test
	public void testFileParsing() throws Exception {
		File file = new File("src/org/eclipse/epsilon/eol/staticanalyser/tests/scripts/" + fileName);
		parseFile(file);
	}

	private void parseFile(File file) throws Exception {
		String content = new String(Files.readAllBytes(file.toPath()));
		String[] lines = content.split("\n");
		List<String> errorMessages = new ArrayList<String>();
		List<String> warningMessages = new ArrayList<String>();
		for (String line: lines) {
			if (!line.substring(0,2).equals("//")){
				break;
			}
		
			if (line.substring(0,3).equals("//!")) {
				errorMessages.add(line.substring(3));
			}
			else if (line.substring(0,3).equals("//?")){
				warningMessages.add(line.substring(3));
			}
		}
		assertValid(content, errorMessages, warningMessages);
	}

	public void assertValid(String eol, List<String> expectedErrorMessages, List<String> expectedWarningMessages)
			throws Exception {
		EolModule module = new EolModule();
		module.parse(eol);
		EolStaticAnalyser staticAnalyser = new EolStaticAnalyser(new StaticModelFactory());
		List<ModuleMarker> markers = staticAnalyser.validate(module);
		List<ModuleMarker> errors = markers.stream().filter(m -> m.getSeverity() == Severity.Error)
				.collect(Collectors.toList());
		List<ModuleMarker> warnings = markers.stream().filter(m -> m.getSeverity() == Severity.Warning)
				.collect(Collectors.toList());

		String errorMessages = errors.stream()
				.map((e) -> e.getMessage() + " line: " + e.getRegion().getStart().getLine())
				.collect(Collectors.joining("\n"));
		assertEquals("Unexpected number of errors\n" + errorMessages + "\n", expectedErrorMessages.size(),
				errors.size());

		Map<String, Integer> errorMessagesMap = new HashMap<String, Integer>();
		for (String errorMessage : errors.stream().map(ModuleMarker::getMessage).collect(Collectors.toList())) {
			if (errorMessagesMap.containsKey(errorMessage)) {
				errorMessagesMap.put(errorMessage, errorMessagesMap.get(errorMessage) + 1);
			} else {
				errorMessagesMap.put(errorMessage, 1);
			}
		}
		for (String errorMessage : expectedErrorMessages) {
			assertTrue("An expected error was not found in the list of thrown errors",
					errorMessagesMap.containsKey(errorMessage));
			errorMessagesMap.put(errorMessage, errorMessagesMap.get(errorMessage) - 1);
		}
		for (Integer i : errorMessagesMap.values()) {
			assertFalse("An error message was not matched enough times", i > 0);
			assertFalse("An error message was matched too many times", i < 0);
		}

		String warningMessages = warnings.stream()
				.map((e) -> e.getMessage() + " line: " + e.getRegion().getStart().getLine())
				.collect(Collectors.joining("\n"));
		assertEquals("Unexpected number of warnings\n" + warningMessages + "\n", expectedWarningMessages.size(),
				warnings.size());

		Map<String, Integer> warningMessagesMap = new HashMap<String, Integer>();
		for (String warningMessage : warnings.stream().map(ModuleMarker::getMessage).collect(Collectors.toList())) {
			if (warningMessagesMap.containsKey(warningMessage)) {
				warningMessagesMap.put(warningMessage, warningMessagesMap.get(warningMessage) + 1);
			} else {
				warningMessagesMap.put(warningMessage, 1);
			}
		}
		for (String warningMessage : expectedWarningMessages) {
			assertTrue("An expected warning was not found in the list of thrown warnings",
					warningMessagesMap.containsKey(warningMessage));
			warningMessagesMap.put(warningMessage, warningMessagesMap.get(warningMessage) - 1);
		}
		for (Integer i : warningMessagesMap.values()) {
			assertFalse("A warning message was not matched enough times", i > 0);
			assertFalse("A warning message was matched too many times", i < 0);
		}

		visit(module.getChildren());
	}

	protected void visit(List<ModuleElement> elements) {
		for (ModuleElement element : elements) {
			// Multiline comments are used to capture the expected type of expressions
			if (!element.getComments().isEmpty() && element.getComments().get(0).isMultiline()) {
				assertEquals(element.getComments().get(0).toString(), getResolvedType(element).toString());
			}
			visit(element.getChildren());
		}
	}

	protected EolType getResolvedType(ModuleElement element) {
		return (EolType) element.getData().get("resolvedType");
	}
}