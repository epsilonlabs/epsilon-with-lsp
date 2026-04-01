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

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.eclipse.epsilon.common.module.IModule;

public class EpsilonTreeAdaptor extends CommonTreeAdaptor {
	
	protected URI uri = null;
	protected IModule module = null;
	
	public EpsilonTreeAdaptor(File file) {
		if (file != null) {
			this.uri = file.toURI();
		}
	}
	
	public EpsilonTreeAdaptor(File file, IModule module) {
		if (file != null) {
			this.uri = file.toURI();
		}
		this.module = module;
	}

	public EpsilonTreeAdaptor(URI uri) {
		this.uri = uri;
	}
	
	public EpsilonTreeAdaptor(URI uri, IModule module) {
		this.uri = uri;
		this.module = module;
	}
	
	@Override
    public AST create(Token token) {
        return new AST(token, uri, module);
    }
	
	@Override
	public Object errorNode(TokenStream input, Token start, Token stop, RecognitionException e) {
		WeakReference<EpsilonParser> parserRef = EpsilonParser.tokenStreamParsers.get(input);
		if (parserRef != null) {
			EpsilonParser parser = parserRef.get();
			if (parser != null) {
				Token replacement = parser.consumeErrorNodeReplacement(e);
				if (replacement != null) {
					return new AST(replacement, uri, module);
				}
				AST recoveredNavigation = recoverNavigationSubtree(input, start, stop, e, parser);
				if (recoveredNavigation != null) {
					return recoveredNavigation;
				}
			}
		}
		return new AST(start, uri, module);
	}

	protected AST recoverNavigationSubtree(TokenStream input, Token start, Token stop, RecognitionException e, EpsilonParser parser) {
		if (!(input instanceof CommonTokenStream) || start == null || stop == null || !parser.shouldReplaceErrorNode(input, e)) {
			return null;
		}
		int nameType = parser.tokenType("NAME");
		int featureCallType = parser.tokenType("FEATURECALL");
		if (nameType < 0 && featureCallType < 0) return null;
		
		List<Token> tokens = collectTokens((CommonTokenStream) input, start, stop);
		if (tokens.isEmpty() || !isNameLike(tokens.get(0), nameType, featureCallType)) {
			return null;
		}
		
		AST currentTree = createAst(copyToken(tokens.get(0)));
		for (int i = 1; i < tokens.size(); i++) {
			Token navigation = tokens.get(i);
			if (!parser.matches(navigation, EpsilonParser.FEATURE_NAVIGATION_TOKENS)) {
				return null;
			}
			
			Token feature = i + 1 < tokens.size() && isNameLike(tokens.get(i + 1), nameType, featureCallType)
				? copyToken(tokens.get(++i))
				: missingNameToken(featureCallType >= 0 ? featureCallType : nameType, navigation);
			
			AST root = createAst(copyToken(navigation));
			addChild(root, currentTree);
			addChild(root, createAst(feature));
			currentTree = root;
		}
		return currentTree;
	}

	protected List<Token> collectTokens(CommonTokenStream input, Token start, Token stop) {
		List<Token> tokens = new ArrayList<>();
		for (int i = start.getTokenIndex(); i <= stop.getTokenIndex(); i++) {
			Token token = input.get(i);
			if (token.getChannel() == Token.DEFAULT_CHANNEL) {
				tokens.add(token);
			}
		}
		return tokens;
	}

	protected CommonToken copyToken(Token token) {
		return token instanceof CommonToken ? new CommonToken((CommonToken) token) : new CommonToken(token);
	}

	protected CommonToken missingNameToken(int nameType, Token navigation) {
		CommonToken missing = new CommonToken(nameType, "");
		missing.setLine(navigation.getLine());
		missing.setCharPositionInLine(navigation.getCharPositionInLine());
		return missing;
	}

	protected boolean isNameLike(Token token, int nameType, int featureCallType) {
		int type = token.getType();
		return type == nameType || type == featureCallType;
	}

	protected AST createAst(Token token) {
		return new AST(token, uri, module);
	}
}
