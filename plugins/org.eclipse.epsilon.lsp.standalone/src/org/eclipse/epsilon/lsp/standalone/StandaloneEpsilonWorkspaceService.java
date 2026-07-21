package org.eclipse.epsilon.lsp.standalone;

import java.io.File;
import java.net.URI;

import org.eclipse.epsilon.lsp.EpsilonWorkspaceService;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.WorkspaceFoldersChangeEvent;

public class StandaloneEpsilonWorkspaceService extends EpsilonWorkspaceService {

	private final StandaloneEpsilonLanguageServer standaloneLanguageServer;

	public StandaloneEpsilonWorkspaceService(StandaloneEpsilonLanguageServer languageServer) {
		super(languageServer);
		standaloneLanguageServer = languageServer;
	}

	@Override
	public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
		WorkspaceFoldersChangeEvent event = params.getEvent();
		event.getAdded().forEach(
			standaloneLanguageServer.getEPackageRegistryManager()::addWorkspaceFolder);
		event.getRemoved().forEach(
			standaloneLanguageServer.getEPackageRegistryManager()::removeWorkspaceFolder);
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		for (FileEvent event : params.getChanges()) {
			if (!event.getUri().endsWith(".emf")) {
				continue;
			}

			File file = new File(URI.create(event.getUri()));
			if (event.getType() == FileChangeType.Changed) {
				standaloneLanguageServer.getEPackageRegistryManager().removeFile(file);
				standaloneLanguageServer.getEPackageRegistryManager().addFile(file);
			}
			else if (event.getType() == FileChangeType.Created) {
				standaloneLanguageServer.getEPackageRegistryManager().addFile(file);
			}
			else if (event.getType() == FileChangeType.Deleted) {
				standaloneLanguageServer.getEPackageRegistryManager().removeFile(file);
			}
		}
	}
}
