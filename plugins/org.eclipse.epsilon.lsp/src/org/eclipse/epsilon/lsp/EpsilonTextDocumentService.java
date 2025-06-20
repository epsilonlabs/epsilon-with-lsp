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
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.emfatic.core.EmfaticResource;
import org.eclipse.emf.emfatic.core.EmfaticResourceFactory;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.ecl.EclModule;
import org.eclipse.epsilon.egl.EglModule;
import org.eclipse.epsilon.egl.EgxModule;
import org.eclipse.epsilon.eml.EmlModule;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.epl.EplModule;
import org.eclipse.epsilon.etl.EtlModule;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.epsilon.flexmi.FlexmiParseException;
import org.eclipse.epsilon.flexmi.FlexmiResource;
import org.eclipse.epsilon.flexmi.FlexmiResourceFactory;
import org.eclipse.epsilon.flock.FlockModule;
import org.eclipse.epsilon.pinset.PinsetModule;
import org.eclipse.gymnast.runtime.core.parser.ParseError;
import org.eclipse.gymnast.runtime.core.parser.ParseMessage;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.services.TextDocumentService;

public class EpsilonTextDocumentService implements TextDocumentService {

    public static final String LANGUAGE_EVL = "evl";
    public static final String LANGUAGE_ETL = "etl";
    public static final String LANGUAGE_EGL = "egl";
    public static final String LANGUAGE_EGX = "egx";
    public static final String LANGUAGE_ECL = "ecl";
    public static final String LANGUAGE_EML = "eml";
    public static final String LANGUAGE_FLOCK = "mig";
    public static final String LANGUAGE_PINSET = "pinset";
    public static final String LANGUAGE_EPL = "epl";
    public static final String LANGUAGE_EOL = "eol";

    protected final Map<String, String> uriLanguageMap = new HashMap<>();
    protected final EpsilonLanguageServer languageServer;
    private static final Logger LOGGER = Logger.getLogger(EpsilonTextDocumentService.class.getName());
    
    public EpsilonTextDocumentService(EpsilonLanguageServer languageServer) {
        this.languageServer = languageServer;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        /*
         * Remember the ID of the language used to edit this file, as the ID is not
         * provided again in didChange.
         */
        final TextDocumentItem doc = params.getTextDocument();
        uriLanguageMap.put(doc.getUri(), doc.getLanguageId());
        publishDiagnostics(doc.getText(), doc.getUri(), doc.getLanguageId());
    }
    
    protected List<Diagnostic> getDiagnostics(EmfaticResource resource, String text) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (ParseMessage parseMessage : resource.getParseContext().getMessages()) {
            Diagnostic diagnostic = new Diagnostic();
            diagnostic.setSeverity(parseMessage instanceof ParseError ? DiagnosticSeverity.Error : DiagnosticSeverity.Warning);
            diagnostic.setMessage(parseMessage.getMessage());

            // TODO: Emfatic produces messages with "at line X, column Y" suffix, which are more accurate than the offset/length

            diagnostic.setRange(new Range(getPosition(text, parseMessage.getOffset()), getPosition(text, parseMessage.getOffset() + parseMessage.getLength())));
            diagnostics.add(diagnostic);
        }
        return diagnostics;
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

    protected List<Diagnostic> getDiagnostics(IEolModule module) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (ParseProblem problem : module.getParseProblems()) {
            Diagnostic diagnostic = new Diagnostic();
            diagnostic.setSeverity(problem.getSeverity() == ParseProblem.ERROR ? DiagnosticSeverity.Error : DiagnosticSeverity.Warning);
            diagnostic.setMessage(problem.getReason());
            diagnostic.setRange(new Range(
                    new Position(problem.getLine() - 1, Math.max(problem.getColumn(),0)),
                    new Position(problem.getLine() - 1, Math.max(problem.getColumn(),0))));
            diagnostics.add(diagnostic);
        }
        return diagnostics;
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        publishDiagnostics(params.getContentChanges().get(0).getText(), 
            params.getTextDocument().getUri(), 
            uriLanguageMap.get(params.getTextDocument().getUri()));
    }

    protected void publishDiagnostics(String code, String uri, String language) {
        IEolModule module = createModule(language);
        List<Diagnostic> diagnostics = Collections.emptyList();

        if (module != null) {
            try {
                module.parse(code, new File(new URI(uri)));
                diagnostics = getDiagnostics(module);
            }
            catch (Exception ex) {
                log(ex);
            }
        }
        else if (language.equals("emfatic")) {
            try {
                ResourceSet resourceSet = new ResourceSetImpl();
                resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("emf", new EmfaticResourceFactory());
                EmfaticResource resource = (EmfaticResource) resourceSet.createResource(org.eclipse.emf.common.util.URI.createURI(uri));
                resource.load(new ByteArrayInputStream(code.getBytes()), null);
                diagnostics = getDiagnostics(resource, code);
            }
            catch (Exception ex) {
                log(ex);
            }
        }
        else if (language.startsWith("flexmi-")) {
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
    public void didClose(DidCloseTextDocumentParams params) {
        uriLanguageMap.remove(params.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        // nothing to do
    }
    
    protected IEolModule createModule(String languageId) {
        switch (languageId) {
            case LANGUAGE_EVL: return new EvlModule();
            case LANGUAGE_ETL: return new EtlModule();
            case LANGUAGE_EGL: return new EglModule();
            case LANGUAGE_EGX: return new EgxModule();
            case LANGUAGE_ECL: return new EclModule();
            case LANGUAGE_EML: return new EmlModule();
            case LANGUAGE_FLOCK:  return new FlockModule();
            case LANGUAGE_PINSET: return new PinsetModule();
            case LANGUAGE_EPL: return new EplModule();
            case LANGUAGE_EOL: return new EolModule();
            default:
                LOGGER.warning("Unknown language " + languageId);
                return null;
        }
    }

    protected void log(Exception ex) {
        languageServer.getClient().logMessage(new MessageParams(MessageType.Error, ex.getMessage()));
    }

}
