/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *******************************************************************************/
package org.eclipse.epsilon.eol.dt.lsp;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.epsilon.common.dt.util.LogUtil;
import org.eclipse.epsilon.lsp.NativeTypeClassLoaderProvider;
import org.eclipse.epsilon.lsp.SingletonMapStreamHandlerService;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;

public class EclipseWorkspaceNativeTypeClassLoaderProvider implements NativeTypeClassLoaderProvider, IResourceChangeListener {

	protected static class CachedClassLoader {
		protected final String classpath;
		protected final ClassLoader parent;
		protected final URLClassLoader classLoader;

		protected CachedClassLoader(String classpath, ClassLoader parent, URLClassLoader classLoader) {
			this.classpath = classpath;
			this.parent = parent;
			this.classLoader = classLoader;
		}
	}

	protected final Map<IProject, CachedClassLoader> classLoaders = new HashMap<IProject, CachedClassLoader>();

	public EclipseWorkspaceNativeTypeClassLoaderProvider() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	@Override
	public synchronized ClassLoader getNativeTypeClassLoader(URI sourceUri, ClassLoader fallbackClassLoader) {
		try {
			IFile sourceFile = getWorkspaceFile(sourceUri);
			if (sourceFile == null) {
				return fallbackClassLoader;
			}

			IJavaProject javaProject = getJavaProject(sourceFile.getProject());
			if (javaProject == null) {
				return fallbackClassLoader;
			}

			// JDT resolves classpath containers here, including PDE's required plug-ins
			// container, which is backed by bundle manifests and the target platform.
			String[] classpathEntries = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
			String classpath = toClasspathString(classpathEntries);
			CachedClassLoader cachedClassLoader = classLoaders.get(javaProject.getProject());
			if (cachedClassLoader != null
					&& cachedClassLoader.parent == fallbackClassLoader
					&& cachedClassLoader.classpath.equals(classpath)) {
				return cachedClassLoader.classLoader;
			}

			URL[] urls = toUrls(classpathEntries);
			if (urls.length == 0) {
				return fallbackClassLoader;
			}

			close(cachedClassLoader);
			URLClassLoader classLoader = new URLClassLoader(urls, fallbackClassLoader);
			classLoaders.put(javaProject.getProject(), new CachedClassLoader(classpath, fallbackClassLoader, classLoader));
			return classLoader;
		}
		catch (CoreException | RuntimeException ex) {
			LogUtil.log(ex);
			return fallbackClassLoader;
		}
	}

	protected IFile getWorkspaceFile(URI sourceUri) {
		URI fileUri = toFileUri(sourceUri);
		if (fileUri == null || !"file".equals(fileUri.getScheme())) {
			return null;
		}

		IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(fileUri);
		if (files == null || files.length == 0) {
			return null;
		}

		for (IFile file : files) {
			if (file.isAccessible()) {
				return file;
			}
		}
		return files[0];
	}

	protected URI toFileUri(URI sourceUri) {
		if (sourceUri == null) {
			return null;
		}
		if ("file".equals(sourceUri.getScheme())) {
			return sourceUri;
		}
		if (SingletonMapStreamHandlerService.PROTOCOL.equals(sourceUri.getScheme())) {
			try {
				return new URI("file", sourceUri.getHost(), sourceUri.getPath(), null);
			}
			catch (URISyntaxException ex) {
				LogUtil.log(ex);
				return null;
			}
		}
		return null;
	}

	protected IJavaProject getJavaProject(IProject project) throws CoreException {
		if (project == null || !project.isAccessible() || !project.hasNature(JavaCore.NATURE_ID)) {
			return null;
		}

		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject == null || !javaProject.exists()) {
			return null;
		}
		return javaProject;
	}

	protected String toClasspathString(String[] classpathEntries) {
		StringBuilder classpath = new StringBuilder();
		if (classpathEntries != null) {
			for (String classpathEntry : classpathEntries) {
				if (classpathEntry == null) {
					continue;
				}
				if (classpath.length() > 0) {
					classpath.append(File.pathSeparatorChar);
				}
				classpath.append(classpathEntry);
			}
		}
		return classpath.toString();
	}

	protected URL[] toUrls(String[] classpathEntries) {
		Set<URL> urls = new LinkedHashSet<URL>();
		if (classpathEntries != null) {
			for (String classpathEntry : classpathEntries) {
				if (classpathEntry == null || classpathEntry.trim().isEmpty()) {
					continue;
				}
				try {
					urls.add(new File(classpathEntry).toURI().toURL());
				}
				catch (MalformedURLException ex) {
					LogUtil.log(ex);
				}
			}
		}
		return urls.toArray(new URL[urls.size()]);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		clearCache();
	}

	public synchronized void clearCache() {
		for (CachedClassLoader cachedClassLoader : classLoaders.values()) {
			close(cachedClassLoader);
		}
		classLoaders.clear();
	}

	protected void close(CachedClassLoader cachedClassLoader) {
		if (cachedClassLoader != null) {
			close(cachedClassLoader.classLoader);
		}
	}

	protected void close(URLClassLoader classLoader) {
		if (classLoader != null) {
			try {
				classLoader.close();
			}
			catch (IOException ex) {
				LogUtil.log(ex);
			}
		}
	}

}
