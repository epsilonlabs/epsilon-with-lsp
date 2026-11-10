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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.source.ContentAssistantFacade;
import org.eclipse.jface.text.source.ISourceViewerExtension4;

/**
 * Installs a {@link CompletionProposalSorter} on the content assistant of the
 * generic editor's viewer.
 *
 * The generic editor offers no way to contribute a proposal sorter, so this is
 * contributed as a reconciler (which is given the viewer on installation) and
 * sets the sorter when a content assist session starts. It does no
 * reconciling itself.
 */
public class CompletionProposalSorterInstaller implements IReconciler {

	protected final ICompletionProposalSorter sorter = new CompletionProposalSorter();
	protected ITextViewer viewer;
	protected ContentAssistantFacade contentAssistant;

	protected final ICompletionListener completionListener = new ICompletionListener() {

		@Override
		public void assistSessionStarted(ContentAssistEvent event) {
			if (event.assistant instanceof ContentAssistant) {
				((ContentAssistant) event.assistant).setSorter(sorter);
			}
		}

		@Override
		public void assistSessionEnded(ContentAssistEvent event) {}

		@Override
		public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {}
	};

	protected final ITextInputListener textInputListener = new ITextInputListener() {

		@Override
		public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {}

		@Override
		public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
			addCompletionListener();
		}
	};

	@Override
	public void install(ITextViewer textViewer) {
		viewer = textViewer;
		// Reconcilers are installed before the viewer creates its content
		// assistant, so this may need to wait until the document is set
		viewer.addTextInputListener(textInputListener);
		addCompletionListener();
	}

	@Override
	public void uninstall() {
		if (viewer != null) {
			viewer.removeTextInputListener(textInputListener);
			viewer = null;
		}
		if (contentAssistant != null) {
			contentAssistant.removeCompletionListener(completionListener);
			contentAssistant = null;
		}
	}

	@Override
	public IReconcilingStrategy getReconcilingStrategy(String contentType) {
		return null;
	}

	protected void addCompletionListener() {
		if (contentAssistant == null && viewer instanceof ISourceViewerExtension4) {
			contentAssistant = ((ISourceViewerExtension4) viewer).getContentAssistantFacade();
			if (contentAssistant != null) {
				contentAssistant.addCompletionListener(completionListener);
			}
		}
	}
}
