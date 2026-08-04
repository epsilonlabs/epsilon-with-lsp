/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.epsilon.lsp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.epsilon.common.module.ModuleElement;
import org.eclipse.epsilon.common.parse.Region;
import org.eclipse.epsilon.egl.IEglModule;
import org.eclipse.epsilon.egl.IEgxModule;
import org.eclipse.epsilon.egl.dom.GenerationRule;
import org.eclipse.epsilon.egl.model.EglMarkerSection;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.dom.Import;
import org.eclipse.epsilon.eol.dom.ModelDeclaration;
import org.eclipse.epsilon.eol.dom.Operation;
import org.eclipse.epsilon.eol.dom.StatementBlock;
import org.eclipse.epsilon.erl.IErlModule;
import org.eclipse.epsilon.erl.dom.NamedRule;
import org.eclipse.epsilon.erl.dom.Post;
import org.eclipse.epsilon.erl.dom.Pre;
import org.eclipse.epsilon.evl.IEvlModule;
import org.eclipse.epsilon.evl.dom.Constraint;
import org.eclipse.epsilon.evl.dom.ConstraintContext;
import org.eclipse.epsilon.evl.dom.GlobalConstraintContext;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;

final class DocumentSymbolExtractor {

	private DocumentSymbolExtractor() {
	}

	static Extraction extract(IEolModule module, String source) {
		return extract(module, source, new ExtractionContext());
	}

	private static Extraction extract(IEolModule module, String source, ExtractionContext context) {
		URI sourceUri = sourceIdentityUri(module);
		if (!context.enter(module, sourceUri)) {
			return Extraction.empty();
		}

		try {
			return extractEnteredModule(module, source, context);
		}
		finally {
			context.exit(module, sourceUri);
		}
	}

	@SuppressWarnings("restriction")
	private static Extraction extractEnteredModule(
			IEolModule module, String source, ExtractionContext context) {
		List<DocumentSymbol> symbols = new ArrayList<>();
		Map<DocumentSymbol, Location> locations = new IdentityHashMap<>();
		Map<Import, DocumentSymbol> importSymbols = new IdentityHashMap<>();
		addCommonDeclarations(module, symbols, importSymbols);

		if (module instanceof IEglModule) {
			addEglDeclarations((IEglModule) module, symbols);
		}
		else if (module instanceof org.eclipse.epsilon.egl.internal.IEglModule) {
			addEglDeclarations((org.eclipse.epsilon.egl.internal.IEglModule) module, symbols);
		}
		else if (module instanceof IEvlModule) {
			addErlDeclarations((IErlModule) module, symbols);
			addEvlDeclarations((IEvlModule) module, symbols);
		}
		else if (module instanceof IEgxModule) {
			addErlDeclarations((IErlModule) module, symbols);
			for (GenerationRule rule : emptyIfNull(((IEgxModule) module).getDeclaredGenerationRules())) {
				add(symbols, namedRuleSymbol(rule, SymbolKind.Function, safeToString(rule)));
			}
		}
		else if (module instanceof IErlModule) {
			addErlDeclarations((IErlModule) module, symbols);
		}
		else {
			StatementBlock main = module.getMain();
			if (main != null) {
				add(symbols, symbol("main", SymbolKind.Function, main, null, null));
			}
		}

		for (Operation operation : emptyIfNull(module.getDeclaredOperations())) {
			if (operation.getNameExpression() == null) {
				continue;
			}
			SymbolKind kind = operation.getContextTypeExpression() == null ? SymbolKind.Function : SymbolKind.Method;
			add(symbols, symbol(operation.getName(), kind, operation, operation.getNameExpression(), operationDetail(operation)));
		}

		sanitize(symbols, source == null ? null : Arrays.asList(source.split("\\r\\n|\\r|\\n", -1)));
		recordLocations(symbols, sourceLocationUri(module), locations);
		addImportedDeclarations(module, symbols, importSymbols, locations, context);
		return new Extraction(symbols, locations);
	}

	private static void addCommonDeclarations(IEolModule module, List<DocumentSymbol> symbols,
			Map<Import, DocumentSymbol> importSymbols) {
		for (Import import_ : emptyIfNull(module.getImports())) {
			if (import_.getPathLiteral() != null) {
				DocumentSymbol importSymbol = symbol(
					import_.getPath(), SymbolKind.Module, import_, import_.getPathLiteral(), "import");
				if (importSymbol != null) {
					symbols.add(importSymbol);
					importSymbols.put(import_, importSymbol);
				}
			}
		}
		for (ModelDeclaration model : emptyIfNull(module.getDeclaredModelDeclarations())) {
			if (model.getNameExpression() == null) {
				continue;
			}
			String detail = model.getDriverNameExpression() == null ? null : model.getDriverNameExpression().getName();
			add(symbols, symbol(model.getNameExpression().getName(), SymbolKind.Object,
				model, model.getNameExpression(), detail));
		}
	}

	private static void addImportedDeclarations(IEolModule module, List<DocumentSymbol> symbols,
			Map<Import, DocumentSymbol> importSymbols, Map<DocumentSymbol, Location> locations,
			ExtractionContext context) {
		for (Import import_ : emptyIfNull(module.getImports())) {
			DocumentSymbol importSymbol = importSymbols.get(import_);
			IEolModule importedModule = loadedImportedModule(import_);
			if (importSymbol == null || !symbols.contains(importSymbol) || importedModule == null) {
				continue;
			}

			Extraction imported = extract(importedModule, null, context);
			List<DocumentSymbol> children = imported.symbols;
			if (!children.isEmpty()) {
				locations.putAll(imported.locations);
				reanchor(children, importSymbol.getRange(), importSymbol.getSelectionRange());
				importSymbol.setChildren(children);
			}
		}
	}

	private static IEolModule loadedImportedModule(Import import_) {
		if (!(import_.getImportedModule() instanceof IEolModule)) {
			return null;
		}

		IEolModule module = (IEolModule) import_.getImportedModule();
		if (!import_.isLoaded() && normalizedSourceUri(module) == null && !hasOpenBuffer(module)) {
			return null;
		}
		try {
			return refreshFromOpenBuffer(module);
		}
		catch (Exception ex) {
			return null;
		}
	}

	private static boolean hasOpenBuffer(IEolModule module) {
		URI sourceUri = normalizedSourceUri(module);
		return sourceUri != null && sourceUri.getPath() != null
			&& MapEntryRegistry.getInstance().getCode(sourceUri.getPath()) != null;
	}

	private static IEolModule refreshFromOpenBuffer(IEolModule module) throws Exception {
		URI sourceUri = normalizedSourceUri(module);
		if (sourceUri == null || MapEntryRegistry.PROTOCOL.equals(sourceUri.getScheme())
				|| !hasOpenBuffer(module)) {
			return module;
		}

		IEolModule refreshed = module.getClass().asSubclass(IEolModule.class)
			.getDeclaredConstructor().newInstance();
		refreshed.setContext(module.getContext());
		refreshed.setParentModule(module.getParentModule());
		refreshed.setImportManager(module.getImportManager());
		refreshed.parse(new URI(MapEntryRegistry.PROTOCOL,
			sourceUri.getAuthority() == null ? "" : sourceUri.getAuthority(),
			sourceUri.getPath(), sourceUri.getQuery(), sourceUri.getFragment()));
		return refreshed;
	}

	private static void addErlDeclarations(IErlModule module, List<DocumentSymbol> symbols) {
		for (Pre pre : emptyIfNull(module.getDeclaredPre())) {
			add(symbols, namedRuleSymbol(pre, SymbolKind.Event, pre.toString()));
		}
		for (Post post : emptyIfNull(module.getDeclaredPost())) {
			add(symbols, namedRuleSymbol(post, SymbolKind.Event, post.toString()));
		}
	}

	private static void addEvlDeclarations(IEvlModule module, List<DocumentSymbol> symbols) {
		for (ConstraintContext context : emptyIfNull(module.getDeclaredConstraintContexts())) {
			if (context instanceof GlobalConstraintContext) {
				for (Constraint constraint : context.getConstraints()) {
					add(symbols, constraintSymbol(constraint));
				}
				continue;
			}

			List<DocumentSymbol> children = new ArrayList<>();
			for (Constraint constraint : context.getConstraints()) {
				add(children, constraintSymbol(constraint));
			}
			DocumentSymbol contextSymbol = symbol(context.getTypeName(), SymbolKind.Class,
				context, context.getTypeExpression(), "context");
			if (contextSymbol == null) {
				symbols.addAll(children);
			}
			else {
				if (!children.isEmpty()) {
					contextSymbol.setChildren(children);
				}
				add(symbols, contextSymbol);
			}
		}
	}

	private static void addEglDeclarations(IEglModule module, List<DocumentSymbol> symbols) {
		if (module.getCurrentTemplate() == null || module.getCurrentTemplate().getModule() == null) {
			return;
		}
		addEglMarkers(module.getCurrentTemplate().getModule().getMarkers(), symbols);
	}

	@SuppressWarnings("restriction")
	private static void addEglDeclarations(
			org.eclipse.epsilon.egl.internal.IEglModule module, List<DocumentSymbol> symbols) {
		addEglMarkers(module.getMarkers(), symbols);
	}

	private static void addEglMarkers(Collection<EglMarkerSection> markers, List<DocumentSymbol> symbols) {
		for (EglMarkerSection marker : emptyIfNull(markers)) {
			String name = safeToString(marker).trim();
			if (name.isEmpty()) {
				name = "marker";
			}
			add(symbols, symbol(name, SymbolKind.String, marker, marker, "marker", -1, 0));
		}
	}

	private static void reanchor(List<DocumentSymbol> symbols, Range range, Range selectionRange) {
		for (DocumentSymbol symbol : symbols) {
			symbol.setRange(copy(range));
			symbol.setSelectionRange(copy(selectionRange));
			if (symbol.getChildren() != null) {
				reanchor(symbol.getChildren(), range, selectionRange);
			}
		}
	}

	private static void recordLocations(List<DocumentSymbol> symbols, URI sourceUri,
			Map<DocumentSymbol, Location> locations) {
		if (sourceUri == null) {
			return;
		}

		for (DocumentSymbol symbol : symbols) {
			locations.put(symbol, new Location(sourceUri.toString(), copy(symbol.getSelectionRange())));
			if (symbol.getChildren() != null) {
				recordLocations(symbol.getChildren(), sourceUri, locations);
			}
		}
	}

	private static DocumentSymbol constraintSymbol(Constraint constraint) {
		if (constraint.getNameExpression() == null) {
			return null;
		}
		return symbol(constraint.getName(), SymbolKind.Method, constraint,
			constraint.getNameExpression(), constraint.isCritique() ? "critique" : "constraint");
	}

	private static DocumentSymbol namedRuleSymbol(NamedRule rule, SymbolKind kind, String detail) {
		String name = rule.getNameExpression() == null ? detail : rule.getName();
		return symbol(name, kind, rule, rule.getNameExpression(), detail);
	}

	private static DocumentSymbol symbol(String name, SymbolKind kind, ModuleElement element,
			ModuleElement selectionElement, String detail) {
		return symbol(name, kind, element, selectionElement, detail, 0, 0);
	}

	private static DocumentSymbol symbol(String name, SymbolKind kind, ModuleElement element,
			ModuleElement selectionElement, String detail, int startColumnOffset, int endColumnOffset) {
		Range range = toRange(element.getRegion(), startColumnOffset, endColumnOffset);
		if (range == null || name == null || name.isEmpty()) {
			return null;
		}

		Range selectionRange = selectionElement == null ? null
			: toRange(selectionElement.getRegion(), startColumnOffset, endColumnOffset);
		if (selectionRange == null || !contains(range, selectionRange)) {
			Position start = range.getStart();
			selectionRange = new Range(new Position(start.getLine(), start.getCharacter()),
				new Position(start.getLine(), start.getCharacter()));
		}

		DocumentSymbol symbol = new DocumentSymbol(name, kind, range, selectionRange);
		if (detail != null && !detail.isEmpty() && !detail.equals(name)) {
			symbol.setDetail(detail);
		}
		return symbol;
	}

	private static Range toRange(Region region, int startColumnOffset, int endColumnOffset) {
		if (region == null || region.getStart() == null || region.getEnd() == null
				|| region.getStart().getLine() < 1 || region.getEnd().getLine() < 1) {
			return null;
		}

		Position start = new Position(region.getStart().getLine() - 1,
			Math.max(region.getStart().getColumn() + startColumnOffset, 0));
		Position end = new Position(region.getEnd().getLine() - 1,
			Math.max(region.getEnd().getColumn() + endColumnOffset, 0));
		if (compare(start, end) > 0) {
			return null;
		}
		return new Range(start, end);
	}

	private static void sanitize(List<DocumentSymbol> symbols, List<String> sourceLines) {
		for (Iterator<DocumentSymbol> iterator = symbols.iterator(); iterator.hasNext();) {
			DocumentSymbol symbol = iterator.next();
			if (sourceLines != null && !clamp(symbol.getRange(), sourceLines)) {
				iterator.remove();
				continue;
			}

			if (sourceLines != null) {
				clamp(symbol.getSelectionRange(), sourceLines);
			}
			ensureSelectionIsContained(symbol);
			if (symbol.getChildren() != null) {
				sanitize(symbol.getChildren(), sourceLines);
			}
		}

		symbols.sort(Comparator
			.comparingInt((DocumentSymbol symbol) -> symbol.getRange().getStart().getLine())
			.thenComparingInt(symbol -> symbol.getRange().getStart().getCharacter()));
		for (int index = 0; index + 1 < symbols.size(); index++) {
			DocumentSymbol current = symbols.get(index);
			Position nextStart = symbols.get(index + 1).getRange().getStart();
			if (compare(current.getRange().getStart(), nextStart) < 0
					&& compare(current.getRange().getEnd(), nextStart) > 0) {
				current.getRange().setEnd(copy(nextStart));
				ensureSelectionIsContained(current);
			}
		}
	}

	private static boolean clamp(Range range, List<String> sourceLines) {
		if (range == null || range.getStart() == null || range.getEnd() == null
				|| sourceLines.isEmpty() || range.getStart().getLine() < 0
				|| range.getStart().getLine() >= sourceLines.size() || range.getEnd().getLine() < 0) {
			return false;
		}

		int startLine = range.getStart().getLine();
		int endLine = Math.min(range.getEnd().getLine(), sourceLines.size() - 1);
		Position start = new Position(startLine,
			Math.min(Math.max(range.getStart().getCharacter(), 0), sourceLines.get(startLine).length()));
		Position end = new Position(endLine,
			Math.min(Math.max(range.getEnd().getCharacter(), 0), sourceLines.get(endLine).length()));
		if (compare(start, end) > 0) {
			return false;
		}
		range.setStart(start);
		range.setEnd(end);
		return true;
	}

	private static void ensureSelectionIsContained(DocumentSymbol symbol) {
		if (symbol.getSelectionRange() == null || !contains(symbol.getRange(), symbol.getSelectionRange())) {
			Position start = symbol.getRange().getStart();
			symbol.setSelectionRange(new Range(copy(start), copy(start)));
		}
	}

	private static Position copy(Position position) {
		return new Position(position.getLine(), position.getCharacter());
	}

	private static Range copy(Range range) {
		return new Range(copy(range.getStart()), copy(range.getEnd()));
	}

	private static boolean contains(Range outer, Range inner) {
		return compare(outer.getStart(), inner.getStart()) <= 0
			&& compare(inner.getEnd(), outer.getEnd()) <= 0;
	}

	private static int compare(Position first, Position second) {
		int lineComparison = Integer.compare(first.getLine(), second.getLine());
		return lineComparison != 0 ? lineComparison : Integer.compare(first.getCharacter(), second.getCharacter());
	}

	private static String safeToString(Object value) {
		try {
			return String.valueOf(value);
		} catch (RuntimeException ex) {
			return "";
		}
	}

	private static String operationDetail(Operation operation) {
		String label = safeToString(operation);
		return label.startsWith(operation.getName()) ? label.substring(operation.getName().length()) : label;
	}

	private static <T> Collection<T> emptyIfNull(Collection<T> elements) {
		return elements == null ? List.of() : elements;
	}

	private static void add(List<DocumentSymbol> symbols, DocumentSymbol symbol) {
		if (symbol != null) {
			symbols.add(symbol);
		}
	}

	private static URI normalizedSourceUri(IEolModule module) {
		try {
			URI uri = module.getSourceUri();
			return uri == null ? null : uri.normalize();
		}
		catch (RuntimeException ex) {
			return null;
		}
	}

	private static URI sourceIdentityUri(IEolModule module) {
		URI sourceUri = normalizedSourceUri(module);
		if (sourceUri == null || !MapEntryRegistry.PROTOCOL.equals(sourceUri.getScheme())) {
			return sourceUri;
		}

		try {
			return new URI("file", sourceUri.getAuthority() == null ? "" : sourceUri.getAuthority(),
				sourceUri.getPath(), sourceUri.getQuery(), sourceUri.getFragment()).normalize();
		}
		catch (URISyntaxException ex) {
			return sourceUri;
		}
	}

	private static URI sourceLocationUri(IEolModule module) {
		URI sourceUri = normalizedSourceUri(module);
		if (sourceUri == null || !MapEntryRegistry.PROTOCOL.equals(sourceUri.getScheme())) {
			return sourceUri;
		}

		try {
			return new URI("file", sourceUri.getAuthority() == null ? "" : sourceUri.getAuthority(),
				sourceUri.getPath(), sourceUri.getQuery(), sourceUri.getFragment());
		}
		catch (URISyntaxException ex) {
			return null;
		}
	}

	static class Extraction {
		final List<DocumentSymbol> symbols;
		final Map<DocumentSymbol, Location> locations;

		Extraction(List<DocumentSymbol> symbols, Map<DocumentSymbol, Location> locations) {
			this.symbols = symbols;
			this.locations = locations;
		}

		static Extraction empty() {
			return new Extraction(Collections.emptyList(), Collections.emptyMap());
		}
	}

	private static class ExtractionContext {
		private final Set<IEolModule> activeModules =
			Collections.newSetFromMap(new IdentityHashMap<IEolModule, Boolean>());
		private final Set<URI> activeUris = new HashSet<>();

		boolean enter(IEolModule module, URI sourceUri) {
			if (activeModules.contains(module) || sourceUri != null && activeUris.contains(sourceUri)) {
				return false;
			}
			activeModules.add(module);
			if (sourceUri != null) {
				activeUris.add(sourceUri);
			}
			return true;
		}

		void exit(IEolModule module, URI sourceUri) {
			activeModules.remove(module);
			if (sourceUri != null) {
				activeUris.remove(sourceUri);
			}
		}
	}
}
