package org.eclipse.epsilon.lsp.standalone;

import java.util.concurrent.CompletableFuture;

import org.eclipse.epsilon.emc.bibtex.BibtexModel;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.analyse.DefaultModelFactory;
import org.eclipse.epsilon.lsp.EpsilonLanguageServer;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;

public class StandaloneEpsilonLanguageServer extends EpsilonLanguageServer {

	private final EmfaticEPackageRegistryManager ePackageRegistryManager =
		new EmfaticEPackageRegistryManager();

	public StandaloneEpsilonLanguageServer() {
		super(
			server -> new StandaloneEpsilonTextDocumentService((StandaloneEpsilonLanguageServer) server),
			server -> new StandaloneEpsilonWorkspaceService((StandaloneEpsilonLanguageServer) server));
		setModelFactory(new DefaultModelFactory()
			.registerModel("EMF", EmfModel::new)
			.registerModel("bibtex", BibtexModel::new));
		addNativeTypeClassLoader(StandaloneEpsilonLanguageServer.class.getClassLoader());
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		if (params.getWorkspaceFolders() != null) {
			ePackageRegistryManager.initialize(params.getWorkspaceFolders());
		}
		return super.initialize(params);
	}

	public EmfaticEPackageRegistryManager getEPackageRegistryManager() {
		return ePackageRegistryManager;
	}
}
