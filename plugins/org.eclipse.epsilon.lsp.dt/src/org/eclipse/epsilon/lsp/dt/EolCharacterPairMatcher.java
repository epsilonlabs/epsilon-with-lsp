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

import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;

/**
 * Highlights matching brackets in the generic editor, as AbstractModuleEditor
 * does in the Epsilon editors. Contributed instead of declaring brackets in the
 * TM4E language configuration, as TM4E would then also auto-close them.
 */
public class EolCharacterPairMatcher extends DefaultCharacterPairMatcher {

	public EolCharacterPairMatcher() {
		super(new char[] { '(', ')', '[', ']', '{', '}' }, IDocumentExtension3.DEFAULT_PARTITIONING);
	}
}
