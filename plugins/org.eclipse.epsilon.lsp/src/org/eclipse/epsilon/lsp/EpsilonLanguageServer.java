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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.eclipse.lsp4j.CompletionOptions;
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
    protected List<String> nativeTypeClasspath = Collections.emptyList();
    protected ClassLoader nativeTypeClassLoader = EpsilonLanguageServer.class.getClassLoader();
    protected URLClassLoader nativeTypeUrlClassLoader = null;
    protected NativeTypeClassLoaderProvider nativeTypeClassLoaderProvider = null;

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
        setNativeTypeClasspath(nativeTypeClasspath);
        configureNativeTypeClasspath(params.getInitializationOptions());
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

    public synchronized ClassLoader getNativeTypeClassLoader() {
        return nativeTypeClassLoader != null ? nativeTypeClassLoader : EpsilonLanguageServer.class.getClassLoader();
    }

    public synchronized ClassLoader getNativeTypeClassLoader(URI sourceUri) {
        ClassLoader fallbackClassLoader = getNativeTypeClassLoader();
        if (nativeTypeClassLoaderProvider == null || sourceUri == null) {
            return fallbackClassLoader;
        }

        ClassLoader sourceClassLoader = nativeTypeClassLoaderProvider.getNativeTypeClassLoader(sourceUri, fallbackClassLoader);
        return sourceClassLoader != null ? sourceClassLoader : fallbackClassLoader;
    }

    public synchronized NativeTypeClassLoaderProvider getNativeTypeClassLoaderProvider() {
        return nativeTypeClassLoaderProvider;
    }

    public synchronized void setNativeTypeClassLoaderProvider(NativeTypeClassLoaderProvider nativeTypeClassLoaderProvider) {
        this.nativeTypeClassLoaderProvider = nativeTypeClassLoaderProvider;
    }

    public synchronized List<String> getNativeTypeClasspath() {
        return new ArrayList<String>(nativeTypeClasspath);
    }

    public void setNativeTypeClasspath(String classpath) {
        setNativeTypeClasspath(parseClasspath(classpath));
    }

    public synchronized void setNativeTypeClasspath(Collection<String> classpathEntries) {
        List<String> cleanedEntries = cleanClasspathEntries(classpathEntries);
        URLClassLoader previousClassLoader = nativeTypeUrlClassLoader;
        nativeTypeClasspath = cleanedEntries;
        URL[] urls = toClasspathUrls(cleanedEntries);
        nativeTypeUrlClassLoader = urls.length == 0
            ? null
            : new URLClassLoader(urls, EpsilonLanguageServer.class.getClassLoader());
        nativeTypeClassLoader = nativeTypeUrlClassLoader != null
            ? nativeTypeUrlClassLoader
            : EpsilonLanguageServer.class.getClassLoader();
        close(previousClassLoader);
    }

    public void configureNativeTypeClasspath(Object options) {
        Object classpath = findClasspathOption(options);
        if (classpath instanceof Collection<?>) {
            setNativeTypeClasspath(toStringList((Collection<?>) classpath));
        }
        else if (classpath instanceof JsonArray) {
            setNativeTypeClasspath(toStringList((JsonArray) classpath));
        }
        else if (classpath instanceof JsonElement) {
            JsonElement jsonClasspath = (JsonElement) classpath;
            if (jsonClasspath.isJsonPrimitive()) {
                setNativeTypeClasspath(jsonClasspath.getAsString());
            }
        }
        else if (classpath instanceof String) {
            setNativeTypeClasspath((String) classpath);
        }
        else if (classpath != null && classpath.getClass().isArray()) {
            List<String> entries = new ArrayList<String>();
            int length = java.lang.reflect.Array.getLength(classpath);
            for (int i = 0; i < length; i++) {
                Object entry = java.lang.reflect.Array.get(classpath, i);
                if (entry != null) {
                    entries.add(entry.toString());
                }
            }
            setNativeTypeClasspath(entries);
        }
    }

    protected Object findClasspathOption(Object options) {
        if (options instanceof String || options instanceof Collection<?> || options instanceof JsonArray
            || (options instanceof JsonElement && ((JsonElement) options).isJsonPrimitive())
            || (options != null && options.getClass().isArray())) {
            return options;
        }
        if (options instanceof JsonObject) {
            JsonObject jsonOptions = (JsonObject) options;
            if (jsonOptions.has("classpath")) {
                return jsonOptions.get("classpath");
            }
            if (jsonOptions.has("java") && jsonOptions.get("java").isJsonObject()) {
                Object classpath = findClasspathOption(jsonOptions.getAsJsonObject("java"));
                if (classpath != null) {
                    return classpath;
                }
            }
            if (jsonOptions.has("epsilon") && jsonOptions.get("epsilon").isJsonObject()) {
                return findClasspathOption(jsonOptions.getAsJsonObject("epsilon"));
            }
            return null;
        }
        if (!(options instanceof Map<?, ?>)) {
            return null;
        }

        Map<?, ?> optionMap = (Map<?, ?>) options;
        if (optionMap.containsKey("classpath")) {
            return optionMap.get("classpath");
        }

        Object javaOptions = optionMap.get("java");
        if (javaOptions instanceof Map<?, ?> && ((Map<?, ?>) javaOptions).containsKey("classpath")) {
            return ((Map<?, ?>) javaOptions).get("classpath");
        }

        Object epsilonOptions = optionMap.get("epsilon");
        if (epsilonOptions instanceof Map<?, ?>) {
            return findClasspathOption(epsilonOptions);
        }
        return null;
    }

    protected List<String> toStringList(JsonArray values) {
        List<String> entries = new ArrayList<String>();
        for (JsonElement value : values) {
            if (value != null && value.isJsonPrimitive()) {
                entries.add(value.getAsString());
            }
        }
        return entries;
    }

    protected List<String> toStringList(Collection<?> values) {
        List<String> entries = new ArrayList<String>();
        for (Object value : values) {
            if (value != null) {
                entries.add(value.toString());
            }
        }
        return entries;
    }

    public static List<String> parseClasspath(String classpath) {
        List<String> entries = new ArrayList<String>();
        if (classpath == null || classpath.trim().isEmpty()) {
            return entries;
        }
        for (String entry : classpath.split("[\\r\\n;]+")) {
            String trimmedEntry = entry.trim();
            if (!trimmedEntry.isEmpty()) {
                entries.add(trimmedEntry);
            }
        }
        return entries;
    }

    protected List<String> cleanClasspathEntries(Collection<String> classpathEntries) {
        List<String> cleanedEntries = new ArrayList<String>();
        if (classpathEntries == null) {
            return cleanedEntries;
        }
        for (String entry : classpathEntries) {
            if (entry != null && !entry.trim().isEmpty()) {
                cleanedEntries.add(entry.trim());
            }
        }
        return cleanedEntries;
    }

    protected URL[] toClasspathUrls(List<String> classpathEntries) {
        Set<URL> urls = new LinkedHashSet<URL>();
        for (String entry : classpathEntries) {
            for (Path path : resolveClasspathEntry(entry)) {
                try {
                    urls.add(path.toUri().toURL());
                } catch (MalformedURLException e) {
                    // Ignore malformed user entries: unresolved classes will still produce diagnostics.
                }
            }
        }
        return urls.toArray(new URL[urls.size()]);
    }

    protected List<Path> resolveClasspathEntry(String entry) {
        List<Path> paths = new ArrayList<Path>();
        String expandedEntry = expandHome(entry);
        Path path = Paths.get(expandedEntry);
        if (path.isAbsolute()) {
            paths.addAll(expandClasspathPath(path.normalize()));
            return paths;
        }

        if (workspaceFolders != null) {
            for (WorkspaceFolder workspaceFolder : workspaceFolders) {
                try {
                    Path workspacePath = Paths.get(URI.create(workspaceFolder.getUri()));
                    paths.addAll(expandClasspathPath(workspacePath.resolve(path).normalize()));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid workspace URIs and keep resolving the remaining entries.
                }
            }
        }
        paths.addAll(expandClasspathPath(path.toAbsolutePath().normalize()));
        return paths;
    }

    protected String expandHome(String entry) {
        if (entry.equals("~")) {
            return System.getProperty("user.home");
        }
        if (entry.startsWith("~/") || entry.startsWith("~" + File.separator)) {
            return System.getProperty("user.home") + entry.substring(1);
        }
        return entry;
    }

    protected List<Path> expandClasspathPath(Path path) {
        String pathString = path.toString();
        if (pathString.indexOf('*') < 0 && pathString.indexOf('?') < 0) {
            return Collections.singletonList(path);
        }

        Path parent = path.getParent();
        Path fileName = path.getFileName();
        if (parent == null || fileName == null || !Files.isDirectory(parent)) {
            return Collections.emptyList();
        }

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + fileName.toString());
        List<Path> matches = new ArrayList<Path>();
        try (Stream<Path> stream = Files.list(parent)) {
            stream.filter(candidate -> matcher.matches(candidate.getFileName()))
                .forEach(candidate -> matches.add(candidate.normalize()));
        } catch (IOException e) {
            // Ignore inaccessible user entries: unresolved classes will still produce diagnostics.
        }
        return matches;
    }

    protected void close(URLClassLoader classLoader) {
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                // Nothing useful to report: the old loader is no longer used.
            }
        }
    }
}
