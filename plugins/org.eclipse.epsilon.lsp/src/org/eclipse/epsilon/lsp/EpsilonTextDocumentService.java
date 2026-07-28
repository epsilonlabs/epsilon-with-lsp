/*******************************************************************************
 * Copyright (c) 2024 The University of York.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.epsilon.lsp;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.epsilon.flexmi.FlexmiParseException;
import org.eclipse.epsilon.flexmi.FlexmiResource;
import org.eclipse.epsilon.flexmi.FlexmiResourceFactory;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DeclarationParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

public class EpsilonTextDocumentService implements TextDocumentService {
    protected final EpsilonLanguageServer languageServer;
    private final Set<String> openDocumentPaths = ConcurrentHashMap.newKeySet();
    
    public EpsilonTextDocumentService(EpsilonLanguageServer languageServer) {
        this.languageServer = languageServer;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        final TextDocumentItem doc = params.getTextDocument();
        
        if (doc.getUri().endsWith(".eol") || doc.getUri().endsWith(".evl") || doc.getUri().endsWith(".egl") || doc.getUri().endsWith(".egx")) {
            URI uri = URI.create(doc.getUri());
            if (uri.getPath() != null) {
                MapEntryRegistry.getInstance().putCode(uri.getPath(), doc.getText());
                openDocumentPaths.add(uri.getPath());
            }
            languageServer.analyser.processDocument(uri);
        }
        else {
        	publishDiagnostics(doc.getText(), doc.getUri(), doc.getLanguageId());
        }
    }
    
    protected List<Diagnostic> getDiagnostics(FlexmiResource resource, String text) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        diagnostics.addAll(getDiagnostics(resource.getWarnings(), DiagnosticSeverity.Warning));
        diagnostics.addAll(getDiagnostics(resource.getErrors(), DiagnosticSeverity.Error));
        return diagnostics;
    }

    protected Collection<Diagnostic> getDiagnostics(List<org.eclipse.emf.ecore.resource.Resource.Diagnostic> emfDiagnostics, DiagnosticSeverity severity) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (org.eclipse.emf.ecore.resource.Resource.Diagnostic emfDiagnostic : emfDiagnostics) {
            Diagnostic diagnostic = new Diagnostic();
            diagnostic.setMessage(emfDiagnostic.getMessage());
            diagnostic.setSeverity(severity);
            Position position = new Position(emfDiagnostic.getLine() - 1, emfDiagnostic.getColumn());
            diagnostic.setRange(new Range(position, position));
            diagnostics.add(diagnostic);
        }
        return diagnostics;
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
    	try {
			languageServer.analyser.checkChangedDocument(new URI(params.getTextDocument().getUri()), params.getContentChanges().get(0).getText());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    protected void publishDiagnostics(String code, String uri, String language) {
        List<Diagnostic> diagnostics = Collections.emptyList();
        
        if (language.startsWith("flexmi-")) {
            try {
                ResourceSet resourceSet = new ResourceSetImpl();
                resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("flexmi", new FlexmiResourceFactory());
                FlexmiResource resource = (FlexmiResource) resourceSet.createResource(org.eclipse.emf.common.util.URI.createURI(uri));
                resource.load(new ByteArrayInputStream(code.getBytes()), null);
                diagnostics = getDiagnostics(resource, code);
            }
            catch (FlexmiParseException fex) {
                Position position = new Position(fex.getLineNumber(), 1);
                Diagnostic diagnostic = new Diagnostic();
                diagnostic.setMessage(fex.getMessage());
                diagnostic.setRange(new Range(position, position));
                diagnostic.setSeverity(DiagnosticSeverity.Error);
                diagnostics = Arrays.asList(diagnostic);
            }
            catch (Exception ex) {
                log(ex);
            }
        }

        final List<Diagnostic> theDiagnostics = diagnostics;
        CompletableFuture.runAsync(() -> {
            languageServer.getClient().publishDiagnostics(new PublishDiagnosticsParams(uri, theDiagnostics));
        });
    }
    
    protected Position getPosition(String code, int offset) {
        int line = 0;
        int column = 0;
        int consumed = 0;

        for (char ch : code.toCharArray()) {
            if (consumed == offset) break;
            consumed ++;
            if (System.lineSeparator().equals(ch + "")) {
                line++;
                column=0;
            }
            else {
                column++;
            }
        }
        return new Position(line, column);
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        final String uriString = params.getTextDocument().getUri();

        // Only the Epsilon languages are handled by the static analyser;
        // for anything else we return an empty completion list so that the
        // client does not keep waiting.
        if (!(uriString.endsWith(".eol") || uriString.endsWith(".evl") || uriString.endsWith(".egl"))) {
            return CompletableFuture.completedFuture(Either.forLeft(Collections.<CompletionItem>emptyList()));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                final List<CompletionItem> items = languageServer.analyser
                    .getCompletions(URI.create(uriString), params.getPosition());
                return Either.<List<CompletionItem>, CompletionList>forLeft(items);
            } catch (Exception ex) {
                log(ex);
                return Either.<List<CompletionItem>, CompletionList>forLeft(Collections.<CompletionItem>emptyList());
            }
        });
    }

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
		final String uriString = params.getTextDocument().getUri();
		if (!(uriString.endsWith(".eol") || uriString.endsWith(".evl") || uriString.endsWith(".egl") || uriString.endsWith(".egx"))) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}

		return CompletableFuture.supplyAsync(() -> {
			try {
				List<DocumentSymbol> symbols = languageServer.analyser.getDocumentSymbols(URI.create(uriString));
				normalizeSymbolKinds(symbols);
				List<Either<SymbolInformation, DocumentSymbol>> result = new ArrayList<>(symbols.size());
				if (languageServer.supportsHierarchicalDocumentSymbols()) {
					for (DocumentSymbol symbol : symbols) {
						result.add(Either.forRight(symbol));
					}
				}
				else {
					addSymbolInformation(result, symbols, uriString, null);
				}
				return result;
			} catch (Exception ex) {
				log(ex);
				return Collections.emptyList();
			}
		});
	}

	private void normalizeSymbolKinds(List<DocumentSymbol> symbols) {
		for (DocumentSymbol symbol : symbols) {
			symbol.setKind(languageServer.getSupportedDocumentSymbolKind(symbol.getKind()));
			if (symbol.getChildren() != null) {
				normalizeSymbolKinds(symbol.getChildren());
			}
		}
	}

	private void addSymbolInformation(List<Either<SymbolInformation, DocumentSymbol>> result,
			List<DocumentSymbol> symbols, String uri, String containerName) {
		for (DocumentSymbol symbol : symbols) {
			SymbolInformation information = new SymbolInformation(symbol.getName(), symbol.getKind(),
				new Location(uri, symbol.getSelectionRange()), containerName);
			result.add(Either.forLeft(information));
			if (symbol.getChildren() != null) {
				addSymbolInformation(result, symbol.getChildren(), uri, symbol.getName());
			}
		}
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> declaration(DeclarationParams params) {
		return declarationOrDefinition(params.getTextDocument().getUri(), params.getPosition());
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
		return declarationOrDefinition(params.getTextDocument().getUri(), params.getPosition());
	}

	private CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> declarationOrDefinition(
			String uriString, Position position) {
		if (!(uriString.endsWith(".eol") || uriString.endsWith(".evl") || uriString.endsWith(".egl") || uriString.endsWith(".egx"))) {
			return CompletableFuture.completedFuture(Either.forLeft(Collections.<Location>emptyList()));
		}

		return CompletableFuture.supplyAsync(() -> {
			try {
				return Either.<List<? extends Location>, List<? extends LocationLink>>forLeft(
					languageServer.analyser.getDeclarations(URI.create(uriString), position));
			} catch (Exception ex) {
				log(ex);
				return Either.<List<? extends Location>, List<? extends LocationLink>>forLeft(Collections.<Location>emptyList());
			}
		});
	}

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
		URI uri = URI.create(params.getTextDocument().getUri());
		if (uri.getPath() != null && openDocumentPaths.remove(uri.getPath())) {
			MapEntryRegistry.getInstance().removeCode(uri.getPath());
		}
    }

	void clearOpenDocuments() {
		for (String path : openDocumentPaths) {
			MapEntryRegistry.getInstance().removeCode(path);
		}
		openDocumentPaths.clear();
	}

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        // nothing to do
    }

    protected void log(Exception ex) {
        languageServer.getClient().logMessage(new MessageParams(MessageType.Error, ex.getMessage()));
    }

}
