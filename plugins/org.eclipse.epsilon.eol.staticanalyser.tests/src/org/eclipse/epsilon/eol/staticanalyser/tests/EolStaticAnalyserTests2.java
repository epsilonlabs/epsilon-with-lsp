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
public class EolStaticAnalyserTests2 {

	private String fileName;

	public EolStaticAnalyserTests2(String fileName) {
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
		assertValid(content);

	}

	public void assertValid(String eol) throws Exception {
		EolModule module = new EolModule();
		module.parse(eol);
		EolStaticAnalyser staticAnalyser = new EolStaticAnalyser(new StaticModelFactory());
		List<ModuleMarker> errors = staticAnalyser.validate(module);
		String messages = errors.stream().map(ModuleMarker::getMessage).collect(Collectors.joining("\n"));
		assertEquals("Unexpected number of errors\n" + messages + "\n", 0, errors.size());
		visit(module.getChildren());
	}

	protected void visit(List<ModuleElement> elements) {
		for (ModuleElement element : elements) {
			if (!element.getComments().isEmpty()) {
				assertEquals(element.getComments().get(0).toString(), getResolvedType(element).toString());
			}
			visit(element.getChildren());
		}
	}

	protected EolType getResolvedType(ModuleElement element) {
		return (EolType) element.getData().get("resolvedType");
	}
}