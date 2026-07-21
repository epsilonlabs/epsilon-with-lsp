package org.eclipse.epsilon.lsp.standalone;

import java.util.concurrent.CompletableFuture;

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
