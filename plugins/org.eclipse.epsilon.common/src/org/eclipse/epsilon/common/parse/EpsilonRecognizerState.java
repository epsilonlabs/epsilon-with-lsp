/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Dimitrios Kolovos - initial API and implementation
 ******************************************************************************/
package org.eclipse.epsilon.common.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;

/**
 * ANTLR passes the RecognizerSharedState of the root parser of a composite
 * grammar to all its delegate parsers. Root parsers should be created with an
 * instance of this class, so that their delegates can share state such as
 * the parse problems they encounter.
 */
public class EpsilonRecognizerState extends RecognizerSharedState {

	protected List<ParseProblem> parseProblems = new ArrayList<>();
	protected Map<RecognitionException, Token> errorNodeReplacements = new HashMap<>();

	public List<ParseProblem> getParseProblems() {
		return parseProblems;
	}

	public Map<RecognitionException, Token> getErrorNodeReplacements() {
		return errorNodeReplacements;
	}
}
