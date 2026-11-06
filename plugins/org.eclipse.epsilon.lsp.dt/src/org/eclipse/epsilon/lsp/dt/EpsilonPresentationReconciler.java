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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;
import org.eclipse.tm4e.ui.themes.ITokenProvider;

/**
 * TextMate presentation reconciler for Epsilon languages that ignores the
 * TM4E theme and colours text using the Epsilon syntax colouring preferences
 * (see {@link EpsilonTokenProvider}).
 */
public class EpsilonPresentationReconciler extends TMPresentationReconciler {

	protected static final Set<EpsilonPresentationReconciler> installed = ConcurrentHashMap.newKeySet();

	public EpsilonPresentationReconciler() {
		// Setting a theme before installation prevents TM4E from
		// looking up a theme for the grammar's scope
		super.setTheme(EpsilonTokenProvider.getCurrent());
	}

	@Override
	public void setTheme(ITokenProvider theme) {
		// TM4E calls this when its own theme preferences change;
		// always stick to the Epsilon colours instead
		super.setTheme(EpsilonTokenProvider.getCurrent());
	}

	@Override
	public void install(ITextViewer viewer) {
		super.install(viewer);
		installed.add(this);
	}

	@Override
	public void uninstall() {
		installed.remove(this);
		super.uninstall();
	}

	/**
	 * Re-colours all open Epsilon editors with the current preferences.
	 */
	public static void refreshAll() {
		Display display = Display.getDefault();
		if (display == null || display.isDisposed()) return;
		display.asyncExec(() -> {
			for (EpsilonPresentationReconciler reconciler : installed) {
				reconciler.setTheme(null);
			}
		});
	}
}
