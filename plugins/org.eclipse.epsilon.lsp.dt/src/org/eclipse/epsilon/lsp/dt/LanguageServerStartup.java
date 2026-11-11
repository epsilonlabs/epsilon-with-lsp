/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.epsilon.lsp.dt;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.epsilon.common.dt.util.LogUtil;
import org.eclipse.lsp4e.ContentTypeToLanguageServerDefinition;
import org.eclipse.lsp4e.LanguageServersRegistry;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.ui.IStartup;

/**
 * Starts the Epsilon language server together with the workbench, rather than
 * when the first Epsilon program is opened in an editor. When it initialises,
 * the server analyses all the Epsilon programs in the workspace, so their
 * problems are reported without having to open any of them first.
 *
 * The server is not started if it has been disabled in the Language Servers
 * preferences, or if the workspace does not contain any Epsilon programs.
 */
public class LanguageServerStartup implements IStartup {

	/** Must match the ID of the server declared in plugin.xml. */
	static final String SERVER_ID = "org.eclipse.epsilon.lsp.server";

	@Override
	public void earlyStartup() {
		List<ContentTypeToLanguageServerDefinition> mappings = LanguageServersRegistry.getInstance()
			.getContentTypeToLSPExtensions().stream()
			.filter(mapping -> SERVER_ID.equals(mapping.getValue().id) && mapping.isUserEnabled())
			.collect(Collectors.toList());
		if (mappings.isEmpty()) {
			return;
		}

		List<IContentType> contentTypes = mappings.stream()
			.map(Map.Entry::getKey).collect(Collectors.toList());
		if (workspaceContainsFileOf(contentTypes)) {
			LanguageServiceAccessor.startLanguageServer(mappings.get(0).getValue());
		}
	}

	/**
	 * Checks the workspace resource tree (which is held in memory, so this does
	 * not access the disk) for a file of any of the given content types.
	 */
	private static boolean workspaceContainsFileOf(List<IContentType> contentTypes) {
		AtomicBoolean found = new AtomicBoolean();
		try {
			ResourcesPlugin.getWorkspace().getRoot().accept(proxy -> {
				if (!found.get() && proxy.getType() == IResource.FILE
						&& isOfContentType(proxy.getName(), contentTypes)) {
					found.set(true);
				}
				// Stop descending into folders once a file has been found
				return !found.get();
			}, IResource.NONE);
		}
		catch (CoreException e) {
			LogUtil.log(e);
			// Fall back to starting the server: the check only avoids
			// starting it unnecessarily
			return true;
		}
		return found.get();
	}

	private static boolean isOfContentType(String fileName, List<IContentType> contentTypes) {
		for (IContentType fileContentType : Platform.getContentTypeManager().findContentTypesFor(fileName)) {
			for (IContentType contentType : contentTypes) {
				if (fileContentType.isKindOf(contentType)) {
					return true;
				}
			}
		}
		return false;
	}
}
