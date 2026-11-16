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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.eol.analyse.DefaultModelFactory;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.DidChangeWatchedFilesCapabilities;
import org.eclipse.lsp4j.DidChangeWatchedFilesRegistrationOptions;
import org.eclipse.lsp4j.DocumentSymbolCapabilities;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.FileSystemWatcher;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.RelativePattern;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

public class EpsilonLanguageServer implements LanguageServer {

    public static final String WATCHED_FILES_METHOD = "workspace/didChangeWatchedFiles";
    public static final String WATCHED_FILES_REGISTRATION_ID = "epsilon.watchedFiles";
    private static final Logger LOGGER = Logger.getLogger(EpsilonLanguageServer.class.getName());

    protected EpsilonTextDocumentService textDocumentService = new EpsilonTextDocumentService(this);
    protected WorkspaceService workspaceService = new EpsilonWorkspaceService(this);
    protected Analyser analyser;
    protected List<ClassLoader> nativeTypeClassLoaders = new ArrayList<ClassLoader>();

    protected AtomicBoolean shutdown = new AtomicBoolean(false);
    protected Consumer<Integer> exitFunction = null;
    protected LanguageClient client;
    protected boolean completionSnippetSupport;
    protected boolean hierarchicalDocumentSymbolSupport;
    protected Set<SymbolKind> supportedDocumentSymbolKinds = defaultDocumentSymbolKinds();
    protected boolean watchedFilesDynamicRegistration;
    protected boolean watchedFilesRelativePatternSupport;

    protected List<WorkspaceFolder> workspaceFolders;
    protected IModelFactory modelFactory = new DefaultModelFactory();

    public EpsilonLanguageServer() {
        this(EpsilonTextDocumentService::new, EpsilonWorkspaceService::new);
    }

    protected EpsilonLanguageServer(
            Function<EpsilonLanguageServer, EpsilonTextDocumentService> textDocumentServiceFactory,
            Function<EpsilonLanguageServer, WorkspaceService> workspaceServiceFactory) {
        addNativeTypeClassLoader(EpsilonLanguageServer.class.getClassLoader());
        textDocumentService = textDocumentServiceFactory.apply(this);
        workspaceService = workspaceServiceFactory.apply(this);
        analyser = new Analyser(this);
    }
    
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
        CompletionCapabilities completionCapabilities = params.getCapabilities() == null
            || params.getCapabilities().getTextDocument() == null
            ? null : params.getCapabilities().getTextDocument().getCompletion();
        completionSnippetSupport = completionCapabilities != null
            && completionCapabilities.getCompletionItem() != null
            && Boolean.TRUE.equals(completionCapabilities.getCompletionItem().getSnippetSupport());
        DocumentSymbolCapabilities documentSymbolCapabilities = params.getCapabilities() == null
            || params.getCapabilities().getTextDocument() == null
            ? null : params.getCapabilities().getTextDocument().getDocumentSymbol();
        hierarchicalDocumentSymbolSupport = documentSymbolCapabilities != null
            && Boolean.TRUE.equals(documentSymbolCapabilities.getHierarchicalDocumentSymbolSupport());
        supportedDocumentSymbolKinds = documentSymbolCapabilities == null
            || documentSymbolCapabilities.getSymbolKind() == null
            || documentSymbolCapabilities.getSymbolKind().getValueSet() == null
            || documentSymbolCapabilities.getSymbolKind().getValueSet().isEmpty()
            ? defaultDocumentSymbolKinds()
            : EnumSet.copyOf(documentSymbolCapabilities.getSymbolKind().getValueSet());
        DidChangeWatchedFilesCapabilities watchedFilesCapabilities = params.getCapabilities() == null
            || params.getCapabilities().getWorkspace() == null
            ? null : params.getCapabilities().getWorkspace().getDidChangeWatchedFiles();
        watchedFilesDynamicRegistration = watchedFilesCapabilities != null
            && Boolean.TRUE.equals(watchedFilesCapabilities.getDynamicRegistration());
        watchedFilesRelativePatternSupport = watchedFilesCapabilities != null
            && Boolean.TRUE.equals(watchedFilesCapabilities.getRelativePatternSupport());
        final InitializeResult res = new InitializeResult(new ServerCapabilities());
        res.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);

        // Advertise completion support. The Epsilon static analyser produces
        // completions purely from AST context, so we do not declare any
        // trigger characters (completion is requested explicitly by the
        // client, e.g. via Ctrl+Space) and we do not need a resolve step.
        final CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setResolveProvider(false);
        res.getCapabilities().setCompletionProvider(completionOptions);
        res.getCapabilities().setDeclarationProvider(true);
        res.getCapabilities().setDefinitionProvider(true);
        res.getCapabilities().setDocumentSymbolProvider(true);
		res.getCapabilities().setExecuteCommandProvider(
			new ExecuteCommandOptions(List.of(EpsilonWorkspaceService.OPEN_DOCUMENT_SYMBOL_COMMAND)));

        analyser = new Analyser(this);
        return CompletableFuture.completedFuture(res);
    }

    /**
     * Analyses the Epsilon programs in the workspace folders and publishes
     * their diagnostics. This is not done in {@link #initialize(InitializeParams)},
     * as the LSP specification does not allow servers to send notifications
     * such as diagnostics before they have responded to the initialize request.
     * The client is also asked to report changes to Epsilon programs on disk,
     * so that their diagnostics can be kept up to date.
     */
    @Override
    public void initialized(InitializedParams params) {
        // Registered first, so that changes made during the analysis are reported
        registerWatchedFiles();
        analyser.initialize();
    }

    /**
     * Asks the client to notify the server of created, changed and deleted
     * Epsilon programs, if the client supports registering this dynamically.
     */
    protected void registerWatchedFiles() {
        if (!watchedFilesDynamicRegistration || client == null) {
            return;
        }

        List<FileSystemWatcher> watchers = new ArrayList<>();
        for (String extension : Analyser.PROGRAM_EXTENSIONS) {
            String pattern = "**/*." + extension;
            if (watchedFilesRelativePatternSupport && workspaceFolders != null) {
                // Patterns relative to each folder do not depend on the client
                // resolving bare patterns against a root (which LSP4E does not
                // do for servers that are not tied to a project)
                for (WorkspaceFolder folder : workspaceFolders) {
                    watchers.add(new FileSystemWatcher(
                        Either.forRight(new RelativePattern(Either.forLeft(folder), pattern))));
                }
            }
            else {
                watchers.add(new FileSystemWatcher(Either.forLeft(pattern)));
            }
        }

        Registration registration = new Registration(WATCHED_FILES_REGISTRATION_ID,
            WATCHED_FILES_METHOD, new DidChangeWatchedFilesRegistrationOptions(watchers));
        try {
            // Not waiting for the response, as it is received by the thread running this
            client.registerCapability(new RegistrationParams(List.of(registration)))
                .exceptionally(ex -> {
                    LOGGER.warning("Failed to register for watched file changes: " + ex.getMessage());
                    return null;
                });
        }
        catch (RuntimeException ex) {
            LOGGER.warning("Failed to register for watched file changes: " + ex.getMessage());
        }
    }

    @Override
    public CompletableFuture<Object> shutdown() {
    	shutdown.set(true);
        textDocumentService.clearOpenDocuments();
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
    
    public synchronized List<ClassLoader> getNativeTypeClassLoaders() {
        return new ArrayList<ClassLoader>(nativeTypeClassLoaders);
    }

    public synchronized void addNativeTypeClassLoader(ClassLoader nativeTypeClassLoader) {
        if (nativeTypeClassLoader != null && !nativeTypeClassLoaders.contains(nativeTypeClassLoader)) {
            nativeTypeClassLoaders.add(nativeTypeClassLoader);
        }
    }

    public Analyser getAnalyser() {
    	return analyser;
    }

    public boolean supportsHierarchicalDocumentSymbols() {
        return hierarchicalDocumentSymbolSupport;
    }

    public boolean supportsCompletionSnippets() {
        return completionSnippetSupport;
    }

    public SymbolKind getSupportedDocumentSymbolKind(SymbolKind preferred) {
        if (supportedDocumentSymbolKinds.contains(preferred)) {
            return preferred;
        }

        SymbolKind fallback = preferred == SymbolKind.Object ? SymbolKind.Variable : SymbolKind.Function;
        if (supportedDocumentSymbolKinds.contains(fallback)) {
            return fallback;
        }
        return supportedDocumentSymbolKinds.iterator().next();
    }

    private static Set<SymbolKind> defaultDocumentSymbolKinds() {
        return EnumSet.range(SymbolKind.File, SymbolKind.Array);
    }

	public IModelFactory getModelFactory() {
		return modelFactory;
	}

	public void setModelFactory(IModelFactory modelFactory) {
		this.modelFactory = modelFactory;
	}

}
