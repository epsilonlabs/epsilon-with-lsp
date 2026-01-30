/*******************************************************************************
 * Copyright (c) 2024 The University of York.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.epsilon.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

public class EpsilonLanguageServer implements LanguageServer {

    protected EpsilonTextDocumentService textDocumentService = new EpsilonTextDocumentService(this);
    protected EPackageRegistryManager ePackageRegistryManager = new EPackageRegistryManager();
    protected WorkspaceService workspaceService = new EpsilonWorkspaceService(this);
    public Analyser analyser = new Analyser(this);

    protected AtomicBoolean shutdown = new AtomicBoolean(false);
    protected Consumer<Integer> exitFunction = null;
    protected LanguageClient client;
    
    protected List<WorkspaceFolder> workspaceFolders;
    
    public void connect(LanguageClient remoteProxy) {
        this.client = remoteProxy;
    }

    public LanguageClient getClient() {
        return client;
    }

    /**
     * Returns the function called to process {@link #exit()} requests.
     * The default is {@code System#exit(int)}, as indicated by the LSP
     * specification.
     */
    public Consumer<Integer> getExitFunction() {
		return exitFunction;
	}

    /**
     * Changes the function called to process {@link #exit()} requests.
     *
     * @see #getExitFunction()
     */
	public void setExitFunction(Consumer<Integer> exitFunction) {
		this.exitFunction = exitFunction;
	}

	@Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		workspaceFolders = params.getWorkspaceFolders();
		if (workspaceFolders != null) ePackageRegistryManager.initialize(params.getWorkspaceFolders());
        final InitializeResult res = new InitializeResult(new ServerCapabilities());
        res.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);
        analyser.initialize();
        return CompletableFuture.completedFuture(res);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
    	shutdown.set(true);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
    	if (exitFunction != null) {
    		exitFunction.accept(shutdown.get() ? 0 : 1);
    	}
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }
    
    public EPackageRegistryManager getEPackageRegistryManager() {
        return ePackageRegistryManager;
    }
}
