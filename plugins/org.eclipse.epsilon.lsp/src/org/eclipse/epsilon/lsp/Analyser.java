package org.eclipse.epsilon.lsp;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.dom.Import;
import org.eclipse.epsilon.eol.staticanalyser.EolStaticAnalyser;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.WorkspaceFolder;

public class Analyser {
//    public static final String LANGUAGE_EVL = "evl";
//    public static final String LANGUAGE_ETL = "etl";
//    public static final String LANGUAGE_EGL = "egl";
//    public static final String LANGUAGE_EGX = "egx";
//    public static final String LANGUAGE_ECL = "ecl";
//    public static final String LANGUAGE_EML = "eml";
//    public static final String LANGUAGE_FLOCK = "mig";
//    public static final String LANGUAGE_PINSET = "pinset";
//    public static final String LANGUAGE_EPL = "epl";
    public static final String LANGUAGE_EOL = "eol";
    private static final Logger LOGGER = Logger.getLogger(Analyser.class.getName());
	protected final EpsilonLanguageServer languageServer;
	private MutableGraph<URI> dependencyGraph = GraphBuilder.directed().build();
	private Map<URI, String> documentRegistry = new HashMap<URI, String>();
	
    public Analyser(EpsilonLanguageServer languageServer) {
        this.languageServer = languageServer;
    }
    
	public void initialize() {
		if (languageServer.workspaceFolders == null)
			return;

		for (WorkspaceFolder wf : languageServer.workspaceFolders) {
			Path path = Paths.get(URI.create(wf.getUri()));
			try (Stream<Path> stream = Files.walk(path)) {
				stream.filter(Files::isRegularFile).filter(f -> f.toString().endsWith(".eol")).forEach(p -> {
					try {
						proccessDocument(p.toUri(), Files.readString(p));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void checkChangedDocument(URI uri, String code) {
		proccessDocument(uri, code);
		for(URI uriDependent : dependencyGraph.predecessors(uri)) {
			String codeDependent = documentRegistry.get(uriDependent);
			proccessDocument(uriDependent, codeDependent);
		}
	}
    
	public void proccessDocument(URI uri, String code){
		documentRegistry.put(uri, code);
		IEolModule module = createModule(FilenameUtils.getExtension(uri.toString()));
		File eolFile = new File(uri);
		List<Diagnostic> diagnostics = Collections.emptyList();
		
		if(module !=null) {
			try {
				module.parse(code, eolFile);
				//build dependency graph
				dependencyGraph.addNode(module.getUri());
				for(Import i : module.getImports()) {
					dependencyGraph.putEdge(module.getUri(), i.getImportedModule().getUri());
				}
//				System.out.println("Dependencies of " + module.getUri().toString() + ": " + dependencyGraph.successors(module.getUri()));

				//parser diagnostics
				diagnostics = getDiagnostics(module);
                if(diagnostics.size() == 0) {
                    EolStaticAnalyser staticAnalyser = new EolStaticAnalyser(new StaticModelFactory());
                    List<ModuleMarker> markers = staticAnalyser.validate(module);
                    diagnostics.addAll(markersToDiagnostics(markers));
                }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
        final List<Diagnostic> theDiagnostics = diagnostics;
        CompletableFuture.runAsync(() -> {
            languageServer.getClient().publishDiagnostics(new PublishDiagnosticsParams(uri.toString(), theDiagnostics));
        });
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
	
    protected IEolModule createModule(String languageId) {
        switch (languageId) {
//            case LANGUAGE_EVL: return new EvlModule();
//            case LANGUAGE_ETL: return new EtlModule();
//            case LANGUAGE_EGL: return new EglModule();
//            case LANGUAGE_EGX: return new EgxModule();
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
