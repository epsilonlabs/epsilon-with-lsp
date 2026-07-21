package org.eclipse.epsilon.lsp.test;

import static org.junit.Assert.assertSame;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.lsp.StaticModelFactory;
import org.junit.Test;

public class GlobalPackageRegistryTest {

	@Test
	public void emfModelsUseTheGlobalPackageRegistry() throws Exception {
		String namespaceUri = "urn:epsilon:lsp:test:global-registry";
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setName("registrytest");
		ePackage.setNsPrefix("registrytest");
		ePackage.setNsURI(namespaceUri);
		EClass eClass = EcoreFactory.eINSTANCE.createEClass();
		eClass.setName("Person");
		ePackage.getEClassifiers().add(eClass);
		EPackage.Registry.INSTANCE.put(namespaceUri, ePackage);

		EmfModel model = (EmfModel) new StaticModelFactory().createModel("EMF");
		Path modelFile = Files.createTempFile("epsilon-lsp-registry", ".model");
		Files.write(modelFile, (
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<registrytest:Person xmi:version=\"2.0\" " +
			"xmlns:xmi=\"http://www.omg.org/XMI\" " +
			"xmlns:registrytest=\"" + namespaceUri + "\"/>")
			.getBytes(StandardCharsets.UTF_8));
		try {
			model.setMetamodelUri(namespaceUri);
			model.setModelFile(modelFile.toString());
			model.loadModelFromUri();
			assertSame(eClass, model.getModelImpl().getContents().get(0).eClass());
		}
		finally {
			if (model.getModelImpl() != null) {
				model.getModelImpl().unload();
			}
			EPackage.Registry.INSTANCE.remove(namespaceUri);
		}
	}
}
