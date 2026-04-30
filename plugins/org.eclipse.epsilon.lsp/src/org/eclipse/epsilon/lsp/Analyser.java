package org.eclipse.epsilon.lsp;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Graphs;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.egl.EglModule;
import org.eclipse.epsilon.egl.EgxModule;
import org.eclipse.epsilon.egl.staticanalyser.EglStaticAnalyser;
import org.eclipse.epsilon.egx.staticanalyser.EgxStaticAnalyser;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.dom.Import;
import org.eclipse.epsilon.eol.staticanalyser.EolCompletion;
import org.eclipse.epsilon.eol.staticanalyser.EolCompletionKind;
import org.eclipse.epsilon.eol.staticanalyser.EolStaticAnalyser;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.epsilon.evl.staticanalyser.EvlStaticAnalyser;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.WorkspaceFolder;

public class Analyser {
    public static final String LANGUAGE_EVL = "evl";
//    public static final String LANGUAGE_ETL = "etl";
    public static final String LANGUAGE_EGL = "egl";
    public static final String LANGUAGE_EGX = "egx";
//    public static final String LANGUAGE_ECL = "ecl";
//    public static final String LANGUAGE_EML = "eml";
//    public static final String LANGUAGE_FLOCK = "mig";
//    public static final String LANGUAGE_PINSET = "pinset";
//    public static final String LANGUAGE_EPL = "epl";
    public static final String LANGUAGE_EOL = "eol";
    private static final Logger LOGGER = Logger.getLogger(Analyser.class.getName());
	protected final EpsilonLanguageServer languageServer;
	private MutableGraph<URI> dependencyGraph = GraphBuilder.directed().build();
	
    public Analyser(EpsilonLanguageServer languageServer) {
        this.languageServer = languageServer;
    }
    
	public void initialize() {
		if (languageServer.workspaceFolders == null)
			return;

		for (WorkspaceFolder wf : languageServer.workspaceFolders) {
			Path path = Paths.get(URI.create(wf.getUri()));
			try (Stream<Path> stream = Files.walk(path)) {
				stream.filter(Files::isRegularFile).filter(f -> f.toString().endsWith(".eol")
						|| f.toString().endsWith(".evl") || f.toString().endsWith(".egl") || f.toString().endsWith(".egx")).forEach(p -> {
							processDocument(p.toUri());
						});

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void checkChangedDocument(URI uri, String code) throws URISyntaxException {
		//Update the in-memory contents of the document
		SingletonMapStreamHandlerService.Registry
		.getInstance()
		.putCode(uri.getPath(), code);
//		//The transitive closure also includes the node itself
		for(URI uriDependent : Graphs.transitiveClosure(dependencyGraph).predecessors(uri)) {
			processDocument( new URI("mapentry", "", uriDependent.getPath(), null));
		}
	}
    
	public void processDocument(URI uri){
		IEolModule module = createModule(FilenameUtils.getExtension(uri.toString()));
		List<Diagnostic> diagnostics = Collections.emptyList();
		
		if(module !=null) {
			try {
				
				module.parse(uri);
				//build dependency graph
				dependencyGraph.addNode(module.getUri());
				for(Import i : module.getImports()) {
					dependencyGraph.putEdge(module.getUri(), i.getImportedModule().getUri());
				}

				//parser diagnostics
				diagnostics = getDiagnostics(module);
                if(diagnostics.size() == 0) {
                    EolStaticAnalyser staticAnalyser;
                    if(module instanceof EvlModule) {
						staticAnalyser = new EvlStaticAnalyser(new StaticModelFactory());
					}
                    else if(module instanceof EglModule) {
                    	staticAnalyser = new EglStaticAnalyser(new StaticModelFactory());
                    }
                    else if(module instanceof EgxModule) {
                    	staticAnalyser = new EgxStaticAnalyser(new StaticModelFactory());
                    }
					else{
						staticAnalyser = new EolStaticAnalyser(new StaticModelFactory());
					}
                    List<ModuleMarker> markers = staticAnalyser.validate(module);
                    diagnostics.addAll(markersToDiagnostics(markers));
                }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 

		String uriString = uri.toString();
		if (uri.getScheme().equals("mapentry")) {
			try {
				uriString = new URI("file","", uri.getPath(),null).toString();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		languageServer.getClient().publishDiagnostics(new PublishDiagnosticsParams(uriString, diagnostics));

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
    
    private List<Diagnostic> markersToDiagnostics(List<ModuleMarker> markers){
    	List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
    	for(ModuleMarker m : markers) {
    		Diagnostic d = new Diagnostic();
    		if(m.getSeverity() == ModuleMarker.Severity.Error) {
    			d.setSeverity(DiagnosticSeverity.Error);
    		}
    		else if(m.getSeverity() == ModuleMarker.Severity.Warning) {
    			d.setSeverity(DiagnosticSeverity.Warning);
    		}
    		else if (m.getSeverity() == ModuleMarker.Severity.Information) {
    			d.setSeverity(DiagnosticSeverity.Information);
    		}
    		d.setMessage(m.getMessage());
    		m.getRegion().getStart().getLine();
    		Position start = new Position(m.getRegion().getStart().getLine() - 1, m.getRegion().getStart().getColumn());
    		Position end = new Position(m.getRegion().getEnd().getLine() - 1, m.getRegion().getEnd().getColumn());
    		d.setRange(new Range(start,end));
    		diagnostics.add(d);
    	}
    	return diagnostics;
    }
	
    public List<CompletionItem> getCompletions(URI fileUri, Position lspPosition) {
        final IEolModule module = createModule(FilenameUtils.getExtension(fileUri.toString()));
        if (module == null) {
            return Collections.emptyList();
        }

        // Always parse through the mapentry scheme so that unsaved buffers
        // are picked up; SingletonMapStreamHandlerService falls back to the
        // file on disk when the registry does not contain the path.
        final URI moduleUri;
        try {
            moduleUri = new URI(SingletonMapStreamHandlerService.PROTOCOL, "", fileUri.getPath(), null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        try {
            module.parse(moduleUri);
        } catch (Exception e) {
            // Parsing can fail while the user is typing; fall through and
            // still try to return completions if a partial AST is available.
            e.printStackTrace();
        }

        final EolStaticAnalyser staticAnalyser;
        if (module instanceof EvlModule) {
            staticAnalyser = new EvlStaticAnalyser(new StaticModelFactory());
        } else if (module instanceof EglModule) {
            staticAnalyser = new EglStaticAnalyser(new StaticModelFactory());
        } else {
            staticAnalyser = new EolStaticAnalyser(new StaticModelFactory());
        }

        // Best-effort validation: it populates "resolvedType" data on the
        // AST, which getVisibleVariables uses to populate the `detail`
        // field. Validation can throw on partially-typed code, in which
        // case we still return the (untyped) completions.
        if (module.getParseProblems().isEmpty()) {
            try {
                staticAnalyser.validate(module);
            } catch (Exception e) {
                LOGGER.warning("Static analysis failed while computing completions: " + e.getMessage());
            }
        }

        // LSP positions are 0-based for both line and character. Epsilon's
        // Position uses 1-based lines and 0-based columns (consistent with
        // how markersToDiagnostics converts the other way around).
        final org.eclipse.epsilon.common.parse.Position epsilonPosition =
            new org.eclipse.epsilon.common.parse.Position(
                lspPosition.getLine() + 1, lspPosition.getCharacter());

        final List<EolCompletion> completions = staticAnalyser.getCompletions(module, epsilonPosition);
        final List<CompletionItem> items = new ArrayList<>(completions.size());
        for (EolCompletion c : completions) {
            final CompletionItem item = new CompletionItem(c.getName());
            item.setKind(toCompletionItemKind(c.getKind()));
            item.setDetail(c.getDetail());
            items.add(item);
        }
        return items;
    }

    private static CompletionItemKind toCompletionItemKind(EolCompletionKind kind) {
        if (kind == null) {
            return CompletionItemKind.Variable;
        }
        switch (kind) {
            // LSP4J does not have a dedicated `Parameter` kind; `Variable`
            // is the conventional fallback used by most language servers.
            case PARAMETER:
            case VARIABLE:
                return CompletionItemKind.Variable;
            case SPECIAL_VARIABLE:
                // self, loopCount, hasMore are language-level contextual
                // identifiers, so we surface them as keywords for clarity.
                return CompletionItemKind.Keyword;
            default:
                return CompletionItemKind.Variable;
        }
    }

    protected IEolModule createModule(String languageId) {
        switch (languageId) {
            case LANGUAGE_EVL: return new EvlModule();
//            case LANGUAGE_ETL: return new EtlModule();
            case LANGUAGE_EGL: return new EglModule();
            case LANGUAGE_EGX: return new EgxModule();
//            case LANGUAGE_ECL: return new EclModule();
//            case LANGUAGE_EML: return new EmlModule();
//            case LANGUAGE_FLOCK:  return new FlockModule();
//            case LANGUAGE_PINSET: return new PinsetModule();
//            case LANGUAGE_EPL: return new EplModule();
            case LANGUAGE_EOL: return new EolModule();
            default:
                LOGGER.warning("Unknown language " + languageId);
                return null;
        }
    }
}
