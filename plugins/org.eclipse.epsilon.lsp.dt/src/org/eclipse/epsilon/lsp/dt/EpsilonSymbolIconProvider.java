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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.epsilon.lsp.EpsilonSymbolDetails;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.lsp4e.outline.SymbolsModel.DocumentSymbolWithURI;
import org.eclipse.lsp4e.ui.LSPImages;
import org.eclipse.lsp4e.ui.SymbolIconProvider;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.SymbolTag;
import org.eclipse.swt.graphics.Image;

/**
 * Shows the icons of the legacy Epsilon editors' outline for the document
 * symbols produced by the Epsilon language server (see DocumentSymbolExtractor
 * in org.eclipse.epsilon.lsp), while keeping LSP4E's overlays
 * (e.g. problem markers).
 */
public class EpsilonSymbolIconProvider extends SymbolIconProvider {

	protected static final String KEY_PREFIX = "org.eclipse.epsilon.lsp.dt.outline.";

	/** Icon of operations (i.e. function/method symbols) */
	protected static final String OPERATION = "operation";

	/** Names of icons that have been looked up but do not exist */
	protected static final Set<String> missingIcons = ConcurrentHashMap.newKeySet();

	@Override
	public Image getImageFor(SymbolKind kind, List<SymbolTag> symbolTags, int severity, Object element) {
		String icon = getIcon(kind, element);
		String key = icon == null ? null : getImageKey(icon);
		if (key == null) {
			return super.getImageFor(kind, symbolTags, severity, element);
		}
		Overlays overlays = getOverlaysFor(kind, symbolTags, severity, element);
		return LSPImages.getImageWithOverlays(key, overlays.topLeft, overlays.topRight,
			overlays.bottomLeft, overlays.bottomRight, overlays.underlay);
	}

	/**
	 * Registers icons/outline/[icon].png in LSP4E's image registry (so that
	 * it can be decorated with overlays by LSPImages) and returns its key,
	 * or null if there is no such icon.
	 */
	protected String getImageKey(String icon) {
		if (missingIcons.contains(icon)) return null;
		String key = KEY_PREFIX + icon;
		ImageRegistry registry = LSPImages.getImageRegistry();
		if (registry.getDescriptor(key) == null) {
			ImageDescriptor descriptor = ResourceLocator
				.imageDescriptorFromBundle(getClass(), "icons/outline/" + icon + ".png").orElse(null);
			if (descriptor == null) {
				missingIcons.add(icon);
				return null;
			}
			registry.put(key, descriptor);
		}
		return key;
	}

	/**
	 * Returns the name of the icon of a symbol. The language server reports
	 * EOL operations as functions/methods, and all other Epsilon module
	 * elements as objects whose detail specifies what they are (see
	 * {@link EpsilonSymbolDetails}); the icon of each such object is named
	 * after its detail. Unknown details have no icon file, so they
	 * fall back to LSP4E's default icons.
	 */
	@SuppressWarnings("restriction") // DocumentSymbolWithURI is x-internal
	protected String getIcon(SymbolKind kind, Object element) {
		if (kind == SymbolKind.Function || kind == SymbolKind.Method) {
			return OPERATION;
		}
		if (kind != SymbolKind.Object) {
			return null;
		}

		DocumentSymbol symbol = null;
		if (element instanceof DocumentSymbolWithURI) {
			symbol = ((DocumentSymbolWithURI) element).symbol;
		}
		else if (element instanceof DocumentSymbol) {
			symbol = (DocumentSymbol) element;
		}
		if (symbol == null) return null;

		String type = EpsilonSymbolDetails.of(symbol.getName(), symbol.getDetail());
		// Guard against details that are not plain icon names
		return type != null && type.matches("[a-z]+") ? type : null;
	}
}
