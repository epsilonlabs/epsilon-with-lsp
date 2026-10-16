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

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.jface.text.source.ContentAssistantFacade;
import org.eclipse.jface.text.source.ISourceViewerExtension4;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

/** Keeps automatic content assist from taking Tab away from snippet placeholders. */
public final class SnippetCompletionSupport implements IStartup, IWindowListener,
		IPartListener2, IWorkbenchListener {

	private static final String GENERIC_EDITOR_ID = "org.eclipse.ui.genericeditor.GenericEditor";
	private static final Set<String> EXTENSIONS = Set.of("eol", "evl", "egl", "egx");
	private final Map<ITextEditor, EditorSupport> editors = new IdentityHashMap<>();
	private final Set<IWorkbenchWindow> windows = Collections.newSetFromMap(new IdentityHashMap<>());
	private IWorkbench workbench;

	@Override
	public void earlyStartup() {
		IWorkbench runningWorkbench = PlatformUI.getWorkbench();
		runningWorkbench.getDisplay().asyncExec(() -> {
			if (runningWorkbench.isClosing()) {
				return;
			}
			workbench = runningWorkbench;
			workbench.addWindowListener(this);
			workbench.addWorkbenchListener(this);
			for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
				windowOpened(window);
			}
		});
	}

	@Override
	public void windowOpened(IWorkbenchWindow window) {
		if (windows.add(window)) {
			window.getPartService().addPartListener(this);
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference reference : page.getEditorReferences()) {
					attach(reference);
				}
			}
		}
	}

	@Override
	public void windowClosed(IWorkbenchWindow window) {
		window.getPartService().removePartListener(this);
		windows.remove(window);
		for (ITextEditor editor : new ArrayList<>(editors.keySet())) {
			if (editor.getSite().getWorkbenchWindow() == window) {
				detach(editor);
			}
		}
	}

	@Override
	public void windowActivated(IWorkbenchWindow window) {
	}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
	}

	@Override
	public void partOpened(IWorkbenchPartReference reference) {
		attach(reference);
	}

	@Override
	public void partActivated(IWorkbenchPartReference reference) {
		attach(reference);
	}

	@Override
	public void partVisible(IWorkbenchPartReference reference) {
		attach(reference);
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference reference) {
		partClosed(reference);
		attach(reference);
	}

	@Override
	public void partClosed(IWorkbenchPartReference reference) {
		IWorkbenchPart part = reference.getPart(false);
		if (part instanceof ITextEditor) {
			detach((ITextEditor) part);
		}
	}

	private void attach(IWorkbenchPartReference reference) {
		if (GENERIC_EDITOR_ID.equals(reference.getId())) {
			IWorkbenchPart part = reference.getPart(false);
			if (part instanceof ITextEditor) {
				attach((ITextEditor) part);
			}
		}
	}

	private void attach(ITextEditor editor) {
		String name = editor.getEditorInput().getName();
		String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		ITextOperationTarget target = editor.getAdapter(ITextOperationTarget.class);
		if (!EXTENSIONS.contains(extension) || !(target instanceof ITextViewer)
				|| !(target instanceof ISourceViewerExtension4)) {
			detach(editor);
			return;
		}
		ITextViewer viewer = (ITextViewer) target;
		ContentAssistantFacade facade = ((ISourceViewerExtension4) target).getContentAssistantFacade();
		EditorSupport existing = editors.get(editor);
		if (existing != null && existing.viewer == viewer && existing.facade == facade) {
			return;
		}
		detach(editor);
		if (facade != null && viewer.getTextWidget() != null && !viewer.getTextWidget().isDisposed()) {
			editors.put(editor, new EditorSupport(editor, viewer, facade));
		}
	}

	private void detach(ITextEditor editor) {
		EditorSupport support = editors.remove(editor);
		if (support != null) {
			support.dispose();
		}
	}

	@Override
	public boolean preShutdown(IWorkbench workbench, boolean forced) {
		return true;
	}

	@Override
	public void postShutdown(IWorkbench workbench) {
		workbench.removeWindowListener(this);
		workbench.removeWorkbenchListener(this);
		for (IWorkbenchWindow window : new ArrayList<>(windows)) {
			windowClosed(window);
		}
		for (ITextEditor editor : new ArrayList<>(editors.keySet())) {
			detach(editor);
		}
		this.workbench = null;
	}

	private final class EditorSupport implements ICompletionListener, ICompletionListenerExtension2,
			ILinkedModeListener, ITextInputListener {

		private final ITextEditor editor;
		private final ITextViewer viewer;
		private final ContentAssistantFacade facade;
		private final Display display;
		private final DisposeListener disposeListener;
		private ContentAssistant assistant;
		private LinkedModeModel model;
		private boolean suppressed;
		private boolean disposed;

		EditorSupport(ITextEditor editor, ITextViewer viewer, ContentAssistantFacade facade) {
			this.editor = editor;
			this.viewer = viewer;
			this.facade = facade;
			display = viewer.getTextWidget().getDisplay();
			disposeListener = event -> detach(editor);
			facade.addCompletionListener(this);
			viewer.addTextInputListener(this);
			viewer.getTextWidget().addDisposeListener(disposeListener);
		}

		@Override
		public void assistSessionStarted(ContentAssistEvent event) {
			if (!disposed && event.assistant instanceof ContentAssistant) {
				if (assistant != event.assistant) {
					restoreActivation();
					assistant = (ContentAssistant) event.assistant;
				}
				updateLinkedMode();
			}
		}

		@Override
		public void applied(ICompletionProposal proposal) {
			try {
				removeDefaultValueProposals();
			}
			catch (BadLocationException e) {
				Platform.getLog(SnippetCompletionSupport.class).error("Cannot initialise snippet navigation", e);
			}
			updateLinkedMode();
		}

		private void removeDefaultValueProposals() throws BadLocationException {
			if (disposed || viewer.getDocument() == null) {
				return;
			}
			LinkedModeModel original = LinkedModeModel.getModel(viewer.getDocument(), viewer.getSelectedRange().x);
			if (original == null || original == model) {
				return;
			}
			boolean hasDefaults = false;
			for (LinkedPosition position : original.getTabStopSequence()) {
				hasDefaults |= isDefaultValueProposal(position);
			}
			if (!hasDefaults) {
				return;
			}

			// LSP4E represents ${1:x} as a one-choice ProposalPosition. Its linked-mode
			// assistant offers "x" immediately, independently of editor auto activation.
			LinkedModeModel replacement = new LinkedModeModel();
			Set<LinkedPositionGroup> copied = Collections.newSetFromMap(new IdentityHashMap<>());
			for (LinkedPosition stop : original.getTabStopSequence()) {
				LinkedPositionGroup group = original.getGroupForPosition(stop);
				if (copied.add(group)) {
					LinkedPositionGroup copy = new LinkedPositionGroup();
					for (LinkedPosition position : group.getPositions()) {
						LinkedPosition clone;
						if (position instanceof ProposalPosition && !isDefaultValueProposal(position)) {
							clone = new ProposalPosition(position.getDocument(), position.getOffset(),
								position.getLength(), position.getSequenceNumber(), ((ProposalPosition) position).getChoices());
						}
						else {
							clone = new LinkedPosition(position.getDocument(), position.getOffset(),
								position.getLength(), position.getSequenceNumber());
						}
						copy.addPosition(clone);
					}
					replacement.addGroup(copy);
				}
			}
			original.exit(ILinkedModeListener.NONE);
			replacement.forceInstall();
			EditorLinkedModeUI ui = new EditorLinkedModeUI(replacement, viewer);
			ui.setCyclingMode(LinkedModeUI.CYCLE_NEVER);
			ui.enter();
		}

		private boolean isDefaultValueProposal(LinkedPosition position) throws BadLocationException {
			if (!(position instanceof ProposalPosition)) {
				return false;
			}
			ICompletionProposal[] choices = ((ProposalPosition) position).getChoices();
			return choices != null && choices.length == 1
				&& position.getContent().equals(choices[0].getDisplayString());
		}

		private void updateLinkedMode() {
			if (disposed) {
				return;
			}
			IDocument document = viewer.getDocument();
			LinkedModeModel active = document == null ? null
				: LinkedModeModel.getModel(document, viewer.getSelectedRange().x);
			if (model != active) {
				if (model != null) {
					model.removeLinkingListener(this);
				}
				model = active;
				if (model != null) {
					model.addLinkingListener(this);
				}
			}
			if (model != null && assistant != null) {
				// Only automatic activation changes; explicit Ctrl+Space remains available.
				assistant.enableAutoActivation(false);
				suppressed = true;
			}
			else {
				restoreActivation();
			}
		}

		private void restoreActivation() {
			if (suppressed) {
				assistant.enableAutoActivation(Platform.getPreferencesService().getBoolean(
					"org.eclipse.ui.genericeditor", "contentAssistant.autoActivation", true, null));
				suppressed = false;
			}
		}

		@Override
		public void left(LinkedModeModel environment, int flags) {
			// Let the linked-mode stack finish unwinding before checking for an outer snippet.
			if (!disposed) {
				display.asyncExec(this::updateLinkedMode);
			}
		}

		@Override
		public void suspend(LinkedModeModel environment) {
		}

		@Override
		public void resume(LinkedModeModel environment, int flags) {
			updateLinkedMode();
		}

		@Override
		public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
			reset();
		}

		@Override
		public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
			display.asyncExec(() -> {
				if (!disposed) {
					attach(editor);
				}
			});
		}

		private void reset() {
			if (model != null) {
				model.removeLinkingListener(this);
				model = null;
			}
			restoreActivation();
			assistant = null;
		}

		void dispose() {
			disposed = true;
			reset();
			facade.removeCompletionListener(this);
			viewer.removeTextInputListener(this);
			if (viewer.getTextWidget() != null && !viewer.getTextWidget().isDisposed()) {
				viewer.getTextWidget().removeDisposeListener(disposeListener);
			}
		}

		@Override
		public void assistSessionEnded(ContentAssistEvent event) {
		}

		@Override
		public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
		}
	}
}
