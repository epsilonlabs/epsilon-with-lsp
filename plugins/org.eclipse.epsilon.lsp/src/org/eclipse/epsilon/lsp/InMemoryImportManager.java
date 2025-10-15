package org.eclipse.epsilon.lsp;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.util.UriUtil;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.IImportManager;
import org.eclipse.epsilon.eol.dom.Import;

public class InMemoryImportManager implements IImportManager {

	private Map<URI, String> documentRegistry;
	
	InMemoryImportManager(Map<URI, String> documentRegistry){
		this.documentRegistry = documentRegistry;
	}
	
	@Override
	public void loadModuleForImport(Import import_, Class<? extends IModule> moduleImplClass, URI baseURI) throws URISyntaxException {
		String importPath = import_.getPath();
		final URI importUri = UriUtil.resolve(importPath, baseURI).normalize();
		File eolFile = new File(importUri);
		String code = documentRegistry.get(importUri);
		final IEolModule parentModule = import_.getParentModule();
		
		try {
			IModule module = moduleImplClass.getDeclaredConstructor().newInstance();
			if (module instanceof IEolModule) {
				IEolModule eolModule = (IEolModule) module;
				eolModule.setImportManager(this);
				eolModule.setParentModule(parentModule);
			}
			import_.setImportedModule(module);
			import_.setLoaded(true);
			module.parse(code, eolFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
