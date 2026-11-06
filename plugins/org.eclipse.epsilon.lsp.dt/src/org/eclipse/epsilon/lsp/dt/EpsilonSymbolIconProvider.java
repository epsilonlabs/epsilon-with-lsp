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

import java.net.URI;
import java.util.List;

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

	protected static final String OPERATION = "operation.gif";
	protected static final String PROGRAM = "program.gif";
	protected static final String IMPORT = "import.gif";
	protected static final String MODEL = "model.gif";
	protected static final String PRE = "pre.gif";
	protected static final String POST = "post.gif";
	protected static final String CONTEXT = "context.gif";
	protected static final String CONSTRAINT = "error.gif";
	protected static final String CRITIQUE = "critique.gif";
	protected static final String GENERATION_RULE = "generationrule.gif";
	protected static final String MARKER = "marker.png";

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
	 * Registers the icon in LSP4E's image registry (so that it can be
	 * decorated with overlays by LSPImages) and returns its key, or null
	 * if the icon cannot be found.
	 */
	protected String getImageKey(String icon) {
		String key = KEY_PREFIX + icon;
		ImageRegistry registry = LSPImages.getImageRegistry();
		if (registry.getDescriptor(key) == null) {
			ImageDescriptor descriptor = ResourceLocator
				.imageDescriptorFromBundle(getClass(), "icons/outline/" + icon).orElse(null);
			if (descriptor == null) return null;
			registry.put(key, descriptor);
		}
		return key;
	}

	/**
	 * Maps a symbol to the corresponding legacy icon, using the kinds/details
	 * assigned to Epsilon module elements by the language server.
	 */
	@SuppressWarnings("restriction") // DocumentSymbolWithURI is x-internal
	protected String getIcon(SymbolKind kind, Object element) {
		DocumentSymbol symbol = null;
		URI uri = null;
		if (element instanceof DocumentSymbolWithURI) {
			symbol = ((DocumentSymbolWithURI) element).symbol;
			uri = ((DocumentSymbolWithURI) element).uri;
		}
		else if (element instanceof DocumentSymbol) {
			symbol = (DocumentSymbol) element;
		}
		if (symbol == null || kind == null) return null;

		String name = symbol.getName() == null ? "" : symbol.getName();
		String detail = symbol.getDetail();

		switch (kind) {
			case Module:
				return IMPORT;
			case Object:
				return MODEL;
			case Event:
				return (detail != null ? detail : name).startsWith("post") ? POST : PRE;
			case Class:
				return CONTEXT;
			case String:
				return MARKER;
			case Method:
				if ("constraint".equals(detail)) return CONSTRAINT;
				if ("critique".equals(detail)) return CRITIQUE;
				return OPERATION;
			case Function:
				// Operations always have a signature as their detail;
				// main blocks and EGX rules have no detail
				if (detail == null) {
					if (isEgx(uri)) return GENERATION_RULE;
					if ("main".equals(name)) return PROGRAM;
				}
				return OPERATION;
			default:
				return null;
		}
	}

	protected boolean isEgx(URI uri) {
		return uri != null && uri.getPath() != null && uri.getPath().toLowerCase().endsWith(".egx");
	}
}
