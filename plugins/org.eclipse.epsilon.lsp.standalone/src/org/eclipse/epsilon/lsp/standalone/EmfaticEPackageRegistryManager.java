package org.eclipse.epsilon.lsp.standalone;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.emfatic.core.EmfaticResourceFactory;
import org.eclipse.lsp4j.WorkspaceFolder;

/**
 * Keeps the global package registry synchronized with workspace Emfatic files.
 */
public class EmfaticEPackageRegistryManager {

	protected final Map<String, List<EPackage>> fileEPackages = new LinkedHashMap<>();

	public void initialize(List<WorkspaceFolder> workspaceFolders) {
		workspaceFolders.forEach(this::addWorkspaceFolder);
	}

	protected List<EPackage> getEPackages(File file) {
		try {
			ResourceSet resourceSet = new ResourceSetImpl();
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("emf", new EmfaticResourceFactory());
			Resource resource = resourceSet.createResource(
				org.eclipse.emf.common.util.URI.createFileURI(file.getAbsolutePath()));
			resource.load(null);

			List<EPackage> ePackages = new ArrayList<>();
			resource.getAllContents().forEachRemaining(element -> {
				if (element instanceof EPackage) {
					ePackages.add((EPackage) element);
				}
			});
			return ePackages;
		}
		catch (Exception ex) {
			return Collections.emptyList();
		}
	}

	public void removeWorkspaceFolder(WorkspaceFolder workspaceFolder) {
		getEmfaticFiles(workspaceFolder).forEach(this::removeFile);
	}

	public void addWorkspaceFolder(WorkspaceFolder workspaceFolder) {
		getEmfaticFiles(workspaceFolder).forEach(this::addFile);
	}

	public void addFile(File file) {
		List<EPackage> ePackages = getEPackages(file);
		fileEPackages.put(file.getAbsolutePath(), ePackages);
		for (EPackage ePackage : ePackages) {
			EPackage.Registry.INSTANCE.put(ePackage.getNsURI(), ePackage);
		}
	}

	public void removeFile(File file) {
		List<EPackage> ePackages = fileEPackages.remove(file.getAbsolutePath());
		if (ePackages != null) {
			ePackages.forEach(ePackage -> EPackage.Registry.INSTANCE.remove(ePackage.getNsURI()));
		}
	}

	protected Collection<File> getEmfaticFiles(WorkspaceFolder workspaceFolder) {
		return FileUtils.listFiles(
			new File(URI.create(workspaceFolder.getUri())), new String[] {"emf"}, true);
	}
}
