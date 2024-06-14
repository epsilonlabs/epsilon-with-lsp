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
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

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
		String firstLine = content.split("\n")[0];
		if (firstLine.substring(0,3).equals("//!")) {
			assertErrorMessage(content, firstLine.substring(3));
		}
		else {
			assertValid(content);
		}
		

	}

	public void assertErrorMessage(String eol, String message) throws Exception {
		EolModule module = new EolModule();
		module.parse(eol);
		EolStaticAnalyser staticAnalyser = new EolStaticAnalyser(new StaticModelFactory());
		List<ModuleMarker> errors = staticAnalyser.validate(module);
		
		assertEquals("unexpected number of errors (" + errors.size() + ") in eol module", 1, errors.size());
		assertEquals(message, errors.get(0).getMessage());
	}
	
	public void assertValid(String eol) throws Exception {
		EolModule module = new EolModule();
		module.parse(eol);
		EolStaticAnalyser staticAnalyser = new EolStaticAnalyser(new StaticModelFactory());
		List<ModuleMarker> markers = staticAnalyser.validate(module);
		List<ModuleMarker> errors = markers.stream().filter(m -> m.getSeverity()==Severity.Error).collect(Collectors.toList());
		List<ModuleMarker> warnings = markers.stream().filter(m -> m.getSeverity()==Severity.Warning).collect(Collectors.toList());
		
		
		String errorMessages = errors.stream().map((e) -> e.getMessage() + " line: " + e.getRegion().getStart().getLine())
				.collect(Collectors.joining("\n"));
		assertEquals("Unexpected number of errors\n" + errorMessages + "\n", 0, errors.size());
		String warningMessages = warnings.stream().map((e) -> e.getMessage() + " line: " + e.getRegion().getStart().getLine())
				.collect(Collectors.joining("\n"));
		assertEquals("Unexpected number of warnings\n" + warningMessages + "\n", 0, warnings.size());
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