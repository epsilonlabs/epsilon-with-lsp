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

import static org.eclipse.epsilon.common.dt.editor.highlighting.EpsilonHighlightingManager.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.epsilon.common.dt.editor.highlighting.EpsilonHighlightingManager;
import org.eclipse.epsilon.common.dt.util.ThemeChangeListener;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.tm4e.core.model.TMToken;
import org.eclipse.tm4e.ui.themes.ITokenProvider;
import org.eclipse.ui.PlatformUI;

/**
 * TM4E token provider that colours TextMate scopes using the colours
 * configured in Preferences &gt; Epsilon &gt; Syntax Coloring (i.e. the
 * same preferences used by the legacy Epsilon editors).
 *
 * Instances are immutable snapshots of the preferences; when the preferences
 * (or the Eclipse light/dark theme) change, a new snapshot is created and
 * pushed to all open {@link EpsilonPresentationReconciler}s.
 */
public class EpsilonTokenProvider implements ITokenProvider {

	/**
	 * A TextMate scope selector (dot-separated segments) mapped to an Epsilon
	 * colour preference and font style. A selector matches a token if all of
	 * its segments appear in the token's type; the most specific
	 * (most segments) matching selector wins.
	 */
	protected static class Rule {
		final Set<String> segments;
		final String preference;
		final int style;

		Rule(String selector, String preference, int style) {
			this.segments = new HashSet<>(Arrays.asList(selector.split("\\.")));
			this.preference = preference;
			this.style = style;
		}
	}

	protected static final List<Rule> RULES = List.of(
		new Rule("comment", COMMENT_COLOR_PREF, SWT.NORMAL),
		new Rule("comment.marker", MARKER_COLOR_PREF, SWT.BOLD),
		new Rule("support.function.annotation", ANNOTATION_COLOR_PREF, SWT.NORMAL),
		new Rule("string", STRING_COLOR_PREF, SWT.NORMAL),
		new Rule("constant.character.escape", STRING_COLOR_PREF, SWT.NORMAL),
		new Rule("string.interpolated", EGL_STATIC_TEXT_COLOR_PREF, SWT.NORMAL),
		new Rule("keyword", KEYWORD_COLOR_PREF, SWT.BOLD),
		new Rule("keyword.other.assertion", ASSERTION_COLOR_PREF, SWT.BOLD),
		new Rule("constant.language", BUILTIN_COLOR_PREF, SWT.ITALIC),
		new Rule("storage.type", TYPE_COLOR_PREF, SWT.BOLD),
		new Rule("entity.name.type", TYPE_COLOR_PREF, SWT.BOLD)
	);

	protected static volatile EpsilonTokenProvider current;
	protected static boolean listenersRegistered = false;

	protected final Map<String, Color> colors = new HashMap<>();
	protected final Map<Rule, IToken> ruleTokens = new HashMap<>();
	protected final IToken defaultToken;
	protected final Map<String, IToken> tokenCache = new ConcurrentHashMap<>();

	/**
	 * Returns a provider reflecting the current Epsilon colour preferences.
	 */
	public static EpsilonTokenProvider getCurrent() {
		EpsilonTokenProvider provider = current;
		if (provider == null) {
			synchronized (EpsilonTokenProvider.class) {
				registerListeners();
				if (current == null) {
					current = new EpsilonTokenProvider();
				}
				provider = current;
			}
		}
		return provider;
	}

	protected EpsilonTokenProvider() {
		EpsilonHighlightingManager manager = new EpsilonHighlightingManager();
		manager.initialiseDefaultColors();
		IPreferenceStore store = manager.getPreferenceStore();
		for (String preference : COLOR_PREFERENCES) {
			colors.put(preference, new Color(PreferenceConverter.getColor(store, preference)));
		}
		for (Rule rule : RULES) {
			ruleTokens.put(rule, new Token(new TextAttribute(colors.get(rule.preference), null, rule.style)));
		}
		defaultToken = new Token(new TextAttribute(colors.get(DEFAULT_COLOR_PREF)));
	}

	protected static void registerListeners() {
		if (listenersRegistered) return;
		listenersRegistered = true;

		EpsilonHighlightingManager manager = new EpsilonHighlightingManager();
		manager.getPreferenceStore().addPropertyChangeListener(event -> {
			if (manager.isColorPreference(event.getProperty())) {
				invalidate();
			}
		});

		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(new ThemeChangeListener() {
				@Override
				public void themeChange() {
					// The constructor of the next snapshot re-initialises the
					// default colours for the new (light/dark) theme
					invalidate();
				}
			});
		}
	}

	/**
	 * Discards the current snapshot and re-colours all open editors.
	 */
	protected static void invalidate() {
		synchronized (EpsilonTokenProvider.class) {
			current = null;
		}
		EpsilonPresentationReconciler.refreshAll();
	}

	@Override
	public IToken getToken(String type) {
		if (type == null || type.isEmpty()) return defaultToken;
		return tokenCache.computeIfAbsent(type, this::computeToken);
	}

	/**
	 * Required by TM4E 0.18+ (deliberately not annotated with @Override
	 * so that this class also compiles against earlier TM4E versions).
	 */
	public IToken getToken(TMToken token) {
		return token == null ? defaultToken : getToken(token.type);
	}

	/**
	 * TM4E encodes the scopes of a token as a dot-separated list of
	 * scope segments (e.g. "source.epsilon.eol.keyword.control"), so we
	 * match rules against the set of segments.
	 */
	protected IToken computeToken(String type) {
		Set<String> segments = new HashSet<>(Arrays.asList(type.split("\\.")));
		Rule best = null;
		for (Rule rule : RULES) {
			if (segments.containsAll(rule.segments) &&
					(best == null || rule.segments.size() > best.segments.size())) {
				best = rule;
			}
		}
		return best == null ? defaultToken : ruleTokens.get(best);
	}

	@Override
	public Color getEditorForeground() {
		return colors.get(DEFAULT_COLOR_PREF);
	}

	// Returning null for the following lets the editor use the standard
	// Eclipse text editor colours

	@Override
	public Color getEditorBackground() {
		return null;
	}

	@Override
	public Color getEditorSelectionBackground() {
		return null;
	}

	@Override
	public Color getEditorSelectionForeground() {
		return null;
	}

	@Override
	public Color getEditorCurrentLineHighlight() {
		return null;
	}
}
