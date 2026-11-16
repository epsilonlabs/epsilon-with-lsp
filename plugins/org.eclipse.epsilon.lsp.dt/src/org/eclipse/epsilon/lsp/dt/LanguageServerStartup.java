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

import org.eclipse.lsp4e.LanguageServersRegistry;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.ui.IStartup;

/**
 * Starts the Epsilon language server together with the workbench, rather than
 * when the first Epsilon program is opened in an editor. When it initialises,
 * the server analyses all the Epsilon programs in the workspace, so their
 * problems are reported without having to open any of them first. It then
 * keeps watching the workspace, so programs that are added or changed later
 * (e.g. by a rebase) are also analysed.
 *
 * The server is started even if the workspace does not contain any Epsilon
 * programs yet, but not if it has been disabled in the Language Servers
 * preferences.
 */
public class LanguageServerStartup implements IStartup {

	/** Must match the ID of the server declared in plugin.xml. */
	static final String SERVER_ID = "org.eclipse.epsilon.lsp.server";

	@Override
	public void earlyStartup() {
		LanguageServersRegistry.getInstance().getContentTypeToLSPExtensions().stream()
			.filter(mapping -> SERVER_ID.equals(mapping.getValue().id) && mapping.isUserEnabled())
			.findFirst()
			.ifPresent(mapping -> LanguageServiceAccessor.startLanguageServer(mapping.getValue()));
	}
}
