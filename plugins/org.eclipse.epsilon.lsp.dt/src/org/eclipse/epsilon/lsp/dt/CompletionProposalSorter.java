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

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;
import org.eclipse.lsp4e.operations.completion.LSCompletionProposal;
import org.eclipse.lsp4e.operations.completion.LSCompletionProposalComparator;

/**
 * Ranks language server completion proposals by how well they match the text
 * typed so far, using the same ordering LSP4E applies when the proposals are
 * first computed.
 *
 * LSP4E only sorts proposals once, when they are computed. As the user keeps
 * typing with the popup open, the content assistant of the generic editor
 * filters the proposals but, without a sorter, keeps their initial order: e.g.
 * after requesting completions at <code>c.</code> and then typing
 * <code>name</code>, <code>eAllContainments</code> stays above
 * <code>name</code>. With this sorter, the popup re-ranks the proposals on
 * every keystroke.
 */
public class CompletionProposalSorter implements ICompletionProposalSorter {

	protected final LSCompletionProposalComparator comparator = new LSCompletionProposalComparator();

	@Override
	public int compare(ICompletionProposal p1, ICompletionProposal p2) {
		boolean ls1 = p1 instanceof LSCompletionProposal;
		boolean ls2 = p2 instanceof LSCompletionProposal;
		if (ls1 && ls2) {
			return comparator.compare((LSCompletionProposal) p1, (LSCompletionProposal) p2);
		}
		// Other proposals (e.g. LSP4E's "incomplete results" placeholder)
		// go after the language server ones, keeping their relative order
		return Boolean.compare(ls2, ls1);
	}
}
