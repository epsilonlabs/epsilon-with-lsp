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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.epsilon.lsp.EpsilonWorkspaceService;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServers;
import org.eclipse.lsp4e.outline.SymbolsModel.DocumentSymbolWithURI;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

@SuppressWarnings("restriction")
final class ImportedDocumentSymbolNavigation {

	private static final String LSP4E_PLUGIN_ID = "org.eclipse.lsp4e";
	private static final String LINK_WITH_EDITOR_PREFERENCE = "org.eclipse.lsp4e.outline.linkWithEditor";
	private static final ImportedDocumentSymbolNavigation INSTANCE = new ImportedDocumentSymbolNavigation();

	private static int installations;
	private final Listener selectionListener = this::selectionChanged;
	private final AtomicLong navigationGeneration = new AtomicLong();
	private boolean filterInstalled;
	private CompletableFuture<?> pendingNavigation = CompletableFuture.completedFuture(null);

	private ImportedDocumentSymbolNavigation() {
	}

	static synchronized void install() {
		installations++;
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(INSTANCE::installFilter);
		}
	}

	static synchronized void uninstall() {
		if (installations > 0 && --installations == 0 && PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(INSTANCE::uninstallFilter);
		}
	}

	private void installFilter() {
		if (!filterInstalled) {
			PlatformUI.getWorkbench().getDisplay().addFilter(SWT.Selection, selectionListener);
			filterInstalled = true;
		}
	}

	private void uninstallFilter() {
		if (filterInstalled) {
			PlatformUI.getWorkbench().getDisplay().removeFilter(SWT.Selection, selectionListener);
			filterInstalled = false;
		}
		navigationGeneration.incrementAndGet();
		pendingNavigation.cancel(true);
		pendingNavigation = CompletableFuture.completedFuture(null);
	}

	private void selectionChanged(Event event) {
		long generation = navigationGeneration.incrementAndGet();
		pendingNavigation.cancel(true);
		if (!(event.item instanceof TreeItem)) {
			return;
		}
		if (!InstanceScope.INSTANCE.getNode(LSP4E_PLUGIN_ID)
				.getBoolean(LINK_WITH_EDITOR_PREFERENCE, true)) {
			return;
		}

		List<DocumentSymbolWithURI> path = symbolPath((TreeItem) event.item);
		if (path.size() < 2) {
			return;
		}

		DocumentSymbolWithURI root = path.get(0);
		if (root == null || root.uri == null || root.symbol.getRange() == null
				|| root.symbol.getRange().getStart() == null || !"import".equals(root.symbol.getDetail())) {
			return;
		}

		List<Integer> childPath = childPath(path);
		if (childPath == null) {
			return;
		}

		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null || window.getActivePage() == null) {
			return;
		}
		IEditorPart editor = window.getActivePage().getActiveEditor();
		if (!(editor instanceof ITextEditor)) {
			return;
		}
		IDocument document = LSPEclipseUtils.getDocument((ITextEditor) editor);
		URI documentUri = document == null ? null : LSPEclipseUtils.toUri(document);
		if (documentUri == null || !documentUri.normalize().equals(root.uri.normalize())) {
			return;
		}

		ExecuteCommandParams command = EpsilonWorkspaceService.createOpenDocumentSymbolCommand(
			root.uri, root.symbol, childPath);
		CompletableFuture<List<Object>> navigation = LanguageServers.forDocument(document)
			.withFilter(capabilities -> capabilities.getExecuteCommandProvider() != null
				&& capabilities.getExecuteCommandProvider().getCommands() != null
				&& capabilities.getExecuteCommandProvider().getCommands()
					.contains(EpsilonWorkspaceService.OPEN_DOCUMENT_SYMBOL_COMMAND))
			.collectAll(server -> server.getWorkspaceService().executeCommand(command));
		pendingNavigation = navigation;
		navigation.thenAccept(results -> openLatest(generation, document, results));
	}

	private void openLatest(long generation, IDocument sourceDocument, List<Object> results) {
		if (navigationGeneration.get() != generation) {
			return;
		}
		Location location = results.stream()
			.map(EpsilonWorkspaceService::toDocumentSymbolLocation)
			.filter(result -> result != null)
			.findFirst().orElse(null);
		if (location == null || !PlatformUI.isWorkbenchRunning()) {
			return;
		}
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			if (!filterInstalled || navigationGeneration.get() != generation
					|| !InstanceScope.INSTANCE.getNode(LSP4E_PLUGIN_ID)
						.getBoolean(LINK_WITH_EDITOR_PREFERENCE, true)) {
				return;
			}
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IEditorPart editor = window == null || window.getActivePage() == null
				? null : window.getActivePage().getActiveEditor();
			if (!(editor instanceof ITextEditor)
					|| LSPEclipseUtils.getDocument((ITextEditor) editor) != sourceDocument) {
				return;
			}
			LSPEclipseUtils.openInEditor(location);
		});
	}

	private static List<DocumentSymbolWithURI> symbolPath(TreeItem selectedItem) {
		List<DocumentSymbolWithURI> path = new ArrayList<>();
		for (TreeItem item = selectedItem; item != null; item = item.getParentItem()) {
			DocumentSymbolWithURI symbol = asDocumentSymbol(item.getData());
			if (symbol == null) {
				return List.of();
			}
			path.add(0, symbol);
		}
		return path;
	}

	private static List<Integer> childPath(List<DocumentSymbolWithURI> symbols) {
		List<Integer> result = new ArrayList<>(symbols.size() - 1);
		DocumentSymbol parent = symbols.get(0).symbol;
		for (int symbolIndex = 1; symbolIndex < symbols.size(); symbolIndex++) {
			DocumentSymbol child = symbols.get(symbolIndex).symbol;
			if (parent.getChildren() == null) {
				return null;
			}

			int childIndex = identityIndexOf(parent.getChildren(), child);
			if (childIndex < 0) {
				return null;
			}
			result.add(childIndex);
			parent = child;
		}
		return result;
	}

	private static int identityIndexOf(List<DocumentSymbol> symbols, DocumentSymbol target) {
		for (int index = 0; index < symbols.size(); index++) {
			if (symbols.get(index) == target) {
				return index;
			}
		}
		return -1;
	}

	private static DocumentSymbolWithURI asDocumentSymbol(Object value) {
		return value instanceof DocumentSymbolWithURI ? (DocumentSymbolWithURI) value : null;
	}
}
