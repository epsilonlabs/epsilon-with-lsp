package org.eclipse.epsilon.lsp;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Graphs;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.epsilon.common.module.ModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.parse.Region;
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
import org.eclipse.epsilon.eol.staticanalyser.EolCompletionParseRepairer;
import org.eclipse.epsilon.eol.staticanalyser.EolStaticAnalyser;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.epsilon.evl.staticanalyser.EvlStaticAnalyser;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Location;
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
		IEolModule module = createModule(FilenameUtils.getExtension(fileUri.toString()));
		if (module == null) {
			return Collections.emptyList();
		}

		// LSP positions are 0-based for both line and character. Epsilon's
		// Position uses 1-based lines and 0-based columns (consistent with
		// how markersToDiagnostics converts the other way around).
		final org.eclipse.epsilon.common.parse.Position epsilonPosition =
			new org.eclipse.epsilon.common.parse.Position(
				lspPosition.getLine() + 1, lspPosition.getCharacter());

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

		IEolModule repairedModule = parseRepairedCompletionModule(fileUri, epsilonPosition);
		if (repairedModule != null) {
			module = repairedModule;
		}

		final EolStaticAnalyser staticAnalyser = createStaticAnalyser(module);

		// Best-effort validation: parser recovery can produce a partial AST even
		// when there are parse problems, and autocomplete should still use any
		// visible-variable snapshots that can be collected from that AST.
		try {
			staticAnalyser.validate(module);
		} catch (Exception e) {
			LOGGER.warning("Static analysis failed while computing completions: " + e.getMessage());
		}

		final List<EolCompletion> completions = staticAnalyser.getCompletions(module, epsilonPosition);
        final List<CompletionItem> items = new ArrayList<>(completions.size());
        for (EolCompletion c : completions) {
            final CompletionItem item = new CompletionItem(toCompletionItemLabel(c));
            item.setKind(toCompletionItemKind(c.getKind()));
            item.setDetail(c.getDetail());
            item.setInsertText(toCompletionItemInsertText(c));
            item.setFilterText(c.getName());
            items.add(item);
        }
		return items;
	}

	public List<Location> getDeclarations(URI fileUri, Position lspPosition) {
		IEolModule module = createModule(FilenameUtils.getExtension(fileUri.toString()));
		if (module == null) {
			return Collections.emptyList();
		}

		final org.eclipse.epsilon.common.parse.Position epsilonPosition =
			new org.eclipse.epsilon.common.parse.Position(
				lspPosition.getLine() + 1, lspPosition.getCharacter());

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
			e.printStackTrace();
		}

		final EolStaticAnalyser staticAnalyser = createStaticAnalyser(module);
		try {
			staticAnalyser.validate(module);
		} catch (Exception e) {
			LOGGER.warning("Static analysis failed while resolving declaration: " + e.getMessage());
		}

		List<Location> locations = new ArrayList<Location>();
		for (ModuleElement declaration : staticAnalyser.getDeclarations(module, epsilonPosition)) {
			Location location = toLocation(declaration, fileUri);
			if (location != null) {
				locations.add(location);
			}
		}
		return locations;
	}

	private Location toLocation(ModuleElement element, URI fallbackUri) {
		Region region = element.getRegion();
		if (region == null || region.getStart() == null || region.getEnd() == null) {
			return null;
		}
		return new Location(toLspUri(element.getUri(), fallbackUri), new Range(
			toLspPosition(region.getStart()), toLspPosition(region.getEnd())));
	}

	private String toLspUri(URI uri, URI fallbackUri) {
		URI targetUri = uri != null ? uri : fallbackUri;
		if (SingletonMapStreamHandlerService.PROTOCOL.equals(targetUri.getScheme())) {
			try {
				return new URI("file", "", targetUri.getPath(), null).toString();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		return targetUri.toString();
	}

	private Position toLspPosition(org.eclipse.epsilon.common.parse.Position position) {
		return new Position(position.getLine() - 1, Math.max(position.getColumn(), 0));
	}

	protected EolStaticAnalyser createStaticAnalyser(IEolModule module) {
		if (module instanceof EvlModule) {
			return new EvlStaticAnalyser(new StaticModelFactory());
		}
		else if (module instanceof EglModule) {
			return new EglStaticAnalyser(new StaticModelFactory());
		}
		else if (module instanceof EgxModule) {
			return new EgxStaticAnalyser(new StaticModelFactory());
		}
		return new EolStaticAnalyser(new StaticModelFactory());
	}

	private static String toCompletionItemLabel(EolCompletion completion) {
		if (completion.getKind() == EolCompletionKind.OPERATION || completion.getKind() == EolCompletionKind.PROPERTY) {
			return completion.getLabel();
		}
		return completion.getName();
	}

	private static String toCompletionItemInsertText(EolCompletion completion) {
		if (completion.getKind() == EolCompletionKind.OPERATION) {
			return completion.getName() + "()";
		}
		return completion.getName();
	}

	protected IEolModule parseRepairedCompletionModule(URI fileUri,
			org.eclipse.epsilon.common.parse.Position epsilonPosition) {
		String code = readDocumentCode(fileUri);
		if (code == null) {
			return null;
		}

		String repairedCode = new EolCompletionParseRepairer().repair(code, epsilonPosition);
		if (repairedCode == null || repairedCode.equals(code)) {
			return null;
		}

		IEolModule repairedModule = createModule(FilenameUtils.getExtension(fileUri.toString()));
		if (repairedModule == null) {
			return null;
		}

		try {
			repairedModule.parse(repairedCode, sourceFileFor(fileUri));
			return repairedModule;
		} catch (Exception e) {
			LOGGER.warning("Failed to parse repaired completion source: " + e.getMessage());
			return null;
		}
	}

	protected String readDocumentCode(URI fileUri) {
		String code = SingletonMapStreamHandlerService.Registry.getInstance().getCode(fileUri.getPath());
		if (code != null) {
			return code;
		}
		try {
			return new String(Files.readAllBytes(Paths.get(fileUri)), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LOGGER.warning("Failed to read completion source: " + e.getMessage());
			return null;
		}
	}

	protected File sourceFileFor(URI fileUri) {
		try {
			return "file".equals(fileUri.getScheme()) ? new File(fileUri) : null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static CompletionItemKind toCompletionItemKind(EolCompletionKind kind) {
        if (kind == null) {
            return CompletionItemKind.Variable;
        }
			switch (kind) {
			case OPERATION:
				return CompletionItemKind.Method;
			case PROPERTY:
				return CompletionItemKind.Property;
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
