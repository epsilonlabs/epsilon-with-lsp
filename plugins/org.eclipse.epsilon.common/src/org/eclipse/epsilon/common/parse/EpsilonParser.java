/*******************************************************************************
 * Copyright (c) 2008 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Dimitrios Kolovos - initial API and implementation
 ******************************************************************************/
package org.eclipse.epsilon.common.parse;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import org.antlr.runtime.BitSet;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.TreeAdaptor;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.common.util.ReflectionUtil;

public abstract class EpsilonParser extends Parser {

	private static final Set<String> RECOVERABLE_DELIMITERS = new HashSet<>(Arrays.asList("';'", "')'", "'}'", "']'"));
	private static final Set<String> RECOVERABLE_PLACEHOLDERS = new HashSet<>(Arrays.asList("NAME"));
	private static final Set<String> SEMICOLON_SYNC_TOKENS = new HashSet<>(Arrays.asList(
		"'}'", "'else'", "'case'", "'default'", "'operation'", "'function'", "'model'", "'import'",
		"'if'", "'for'", "'while'", "'switch'", "'return'", "'throw'", "'delete'", "'break'",
		"'breakAll'", "'continue'", "'abort'", "'transaction'", "'var'", "'ext'", "'new'",
		"NAME", "INT", "FLOAT", "STRING", "BOOLEAN", "CollectionTypeName", "MapTypeName",
		"SpecialTypeName", "Annotation", "EOF"
	));
	private static final Set<String> RPAREN_SYNC_TOKENS = new HashSet<>(Arrays.asList(
		"'{'", "';'", "')'", "'}'", "']'", "','", "'else'", "'case'", "'default'", "EOF"
	));
	private static final Set<String> RBRACE_SYNC_TOKENS = new HashSet<>(Arrays.asList(
		"'}'", "'else'", "'case'", "'default'", "'operation'", "'function'", "'model'", "'import'", "EOF"
	));
	private static final Set<String> RBRACKET_SYNC_TOKENS = new HashSet<>(Arrays.asList(
		"'.'", "'?.'", "'->'", "';'", "')'", "']'", "'}'", "','", "EOF"
	));
	private static final Set<String> FEATURE_NAME_SYNC_TOKENS = new HashSet<>(Arrays.asList(
		"';'", "')'", "']'", "'}'", "','", "EOF"
	));
	static final Set<String> FEATURE_NAVIGATION_TOKENS = new HashSet<>(Arrays.asList(
		"POINT", "NAVIGATION", "ARROW", "'.'", "'?.'", "'->'"
	));
	
	private boolean printErrors = false;
	protected List<ParseProblem> parseProblems = new ArrayList<ParseProblem>();
	protected static WeakHashMap<TokenStream, WeakReference<EpsilonParser>> tokenStreamParsers = new WeakHashMap<TokenStream, WeakReference<EpsilonParser>>();
	protected WeakHashMap<RecognitionException, Token> errorNodeReplacements = new WeakHashMap<>();
	protected EpsilonParser delegator = null;
	
	public EpsilonParser(TokenStream tokenstream) {
		super(tokenstream);
		configureDelegator(tokenstream);
	}

	public EpsilonParser(TokenStream tokenstream, RecognizerSharedState recognizersharedstate) {
		super(tokenstream, recognizersharedstate);
		configureDelegator(tokenstream);
	}
	
	protected void configureDelegator(TokenStream tokenStream) {
		// A parser can spawn several delegates on the same token stream
		// The first parser to process a token stream is recorded in tokenStreamParsers 
		// so that all delegates can report parse problems to it (their delegator)
		
		if (!tokenStreamParsers.containsKey(tokenStream)) {
			tokenStreamParsers.put(tokenStream, new WeakReference<EpsilonParser>(this));
			delegator = this;
		}
		else {
			WeakReference<EpsilonParser> ref = tokenStreamParsers.get(tokenStream);
			EpsilonParser parser = ref.get();
			if (parser != null) {
				delegator = parser;
			}
			else {
				delegator = this;
			}
		}
	}
	
	public abstract TreeAdaptor getTreeAdaptor();
	
	public abstract void setTreeAdaptor(TreeAdaptor adaptor);
	
	protected boolean isSupertype(Class<?> parent, Class<?> child) {
		if (parent == child) {
			return true;
		}
		else if (child.getSuperclass() != null) {
			return isSupertype(parent, child.getSuperclass());
		}
		else {
			return false;
		}
	}
	
	public void setDeepTreeAdaptor(TreeAdaptor adaptor) {
		
		setTreeAdaptor(adaptor);
		
		for (Field f : this.getClass().getFields()) {
			try {
				if (isSupertype(EpsilonParser.class, f.getType())) {
					Object value = ReflectionUtil.getFieldValue(this, f.getName());
					EpsilonParser delegate = (EpsilonParser) value;
					if (delegate.getTreeAdaptor() != adaptor) {
						delegate.setDeepTreeAdaptor(adaptor);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void prepareForGUnit() {
		printErrors = true;
	}
	
	@Override
	public void displayRecognitionError(String[] tokenNames, RecognitionException re) {
		reportException(re.line, re.charPositionInLine, getErrorMessage(re, getTokenNames()));
		
		if (printErrors) {
			super.displayRecognitionError(tokenNames, re);
		}
	}
	
	public void reportException(int line, int column, String reason) {
		// Filter out duplicate problems
		if (!delegator.getParseProblems().stream().anyMatch(
				problem -> problem.getColumn() == column && 
				problem.getLine() == line && 
				problem.getReason().equals(reason))) {
			
			ParseProblem problem = new ParseProblem();
			problem.setLine(line);
			problem.setColumn(column);
			problem.setReason(reason);
			delegator.getParseProblems().add(problem);
		}
	}
	
	public List<ParseProblem> getParseProblems() {
		return parseProblems;
	}

	@Override
	public Object recoverFromMismatchedToken(IntStream input, int ttype, BitSet follow) throws RecognitionException {
		if (input instanceof TokenStream) {
			Token current = ((TokenStream) input).LT(1);
			if (shouldInsertMissingToken(current, ttype, follow)) {
				Token missing = (Token) getMissingSymbol(input, null, ttype, follow);
				reportMissingToken(current, missing, ttype);
				return missing;
			}
		}
		return super.recoverFromMismatchedToken(input, ttype, follow);
	}

	@Override
	public void recover(IntStream input, RecognitionException re) {
		if (shouldReplaceErrorNode(input, re)) {
			Token current = ((TokenStream) input).LT(1);
			int nameType = tokenType("NAME");
			Token missing = nameType > 0
				? (Token) getMissingSymbol(input, re, nameType, null)
				: new CommonToken(Token.INVALID_TOKEN_TYPE, "");
			reportMissingToken(current, missing, nameType);
			errorNodeReplacements.put(re, missing);
			state.failed = false;
			return;
		}
		super.recover(input, re);
	}

	protected boolean shouldInsertMissingToken(Token current, int expectedType, BitSet follow) {
		if (current == null) return false;
		String expectedName = tokenName(expectedType);
		if (RECOVERABLE_PLACEHOLDERS.contains(expectedName)) {
			return shouldInsertMissingPlaceholder(current, expectedName);
		}
		if (!RECOVERABLE_DELIMITERS.contains(expectedName)) return false;
		if (current.getType() == Token.EOF) return true;
		if (follow != null && follow.member(current.getType())) return true;
		String currentName = tokenName(current.getType());
		String currentTextKey = current.getText() != null ? "'" + current.getText() + "'" : currentName;
		switch (expectedName) {
			case "';'":
				return SEMICOLON_SYNC_TOKENS.contains(currentName) || SEMICOLON_SYNC_TOKENS.contains(currentTextKey);
			case "')'":
				return RPAREN_SYNC_TOKENS.contains(currentName) || RPAREN_SYNC_TOKENS.contains(currentTextKey);
			case "'}'":
				return RBRACE_SYNC_TOKENS.contains(currentName) || RBRACE_SYNC_TOKENS.contains(currentTextKey);
			case "']'":
				return RBRACKET_SYNC_TOKENS.contains(currentName) || RBRACKET_SYNC_TOKENS.contains(currentTextKey);
			default:
				return false;
		}
	}

	protected boolean shouldInsertMissingPlaceholder(Token current, String expectedName) {
		switch (expectedName) {
			case "NAME":
				return isFeatureNameRecoveryPoint(current);
			default:
				return false;
		}
	}

	protected boolean shouldReplaceErrorNode(IntStream input, RecognitionException re) {
		if (!(re instanceof org.antlr.runtime.NoViableAltException) || !(input instanceof TokenStream)) {
			return false;
		}
		Token current = ((TokenStream) input).LT(1);
		return isFeatureNameRecoveryPoint(current);
	}

	protected boolean isFeatureNameRecoveryPoint(Token current) {
		Token previous = input != null ? input.LT(-1) : null;
		if (previous == null || !matches(previous, FEATURE_NAVIGATION_TOKENS)) {
			return false;
		}
		return current.getType() == Token.EOF || matches(current, FEATURE_NAME_SYNC_TOKENS);
	}

	protected boolean matches(Token token, Set<String> candidates) {
		if (token == null) return false;
		String tokenName = tokenName(token.getType());
		if (candidates.contains(tokenName)) return true;
		String tokenText = token.getText();
		return tokenText != null && candidates.contains("'" + tokenText + "'");
	}

	protected void reportMissingToken(Token current, Token missing, int expectedType) {
		String expectedName = tokenName(expectedType);
		String missingText = missing != null && missing.getText() != null ? missing.getText() : stripQuotes(expectedName);
		if (missingText == null || missingText.isEmpty()) {
			missingText = stripQuotes(expectedName);
		}
		String currentText = current != null && current.getText() != null ? current.getText() : tokenName(current != null ? current.getType() : Token.EOF);
		int line = current != null ? current.getLine() : 0;
		int column = current != null ? current.getCharPositionInLine() : 0;
		reportException(line, column, "missing " + missingText + " before '" + currentText + "'");
	}

	Token consumeErrorNodeReplacement(RecognitionException re) {
		return errorNodeReplacements.remove(re);
	}

	@Override
	protected Object getMissingSymbol(IntStream input, RecognitionException e, int expectedTokenType, BitSet follow) {
		String expectedName = tokenName(expectedTokenType);
		if (RECOVERABLE_PLACEHOLDERS.contains(expectedName) && input instanceof TokenStream) {
			Token current = ((TokenStream) input).LT(1);
			CommonToken missing = new CommonToken(expectedTokenType, "");
			if (current != null) {
				missing.setLine(current.getLine());
				missing.setCharPositionInLine(current.getCharPositionInLine());
			}
			return missing;
		}
		return super.getMissingSymbol(input, e, expectedTokenType, follow);
	}

	protected String tokenName(int type) {
		if (type == Token.EOF) return "EOF";
		String[] tokenNames = getTokenNames();
		if (tokenNames == null || type < 0 || type >= tokenNames.length) {
			return String.valueOf(type);
		}
		return tokenNames[type];
	}

	protected int tokenType(String tokenName) {
		String[] tokenNames = getTokenNames();
		if (tokenNames == null || tokenName == null) return -1;
		for (int i = 0; i < tokenNames.length; i++) {
			if (tokenName.equals(tokenNames[i])) {
				return i;
			}
		}
		return -1;
	}

	protected String stripQuotes(String tokenName) {
		if (tokenName != null && tokenName.length() >= 2 && tokenName.startsWith("'") && tokenName.endsWith("'")) {
			return tokenName.substring(1, tokenName.length() - 1);
		}
		return tokenName;
	}
}
