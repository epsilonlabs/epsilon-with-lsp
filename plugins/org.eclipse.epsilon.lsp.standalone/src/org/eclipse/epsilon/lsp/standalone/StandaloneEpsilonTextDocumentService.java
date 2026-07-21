package org.eclipse.epsilon.lsp.standalone;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.emfatic.core.EmfaticResource;
import org.eclipse.emf.emfatic.core.EmfaticResourceFactory;
import org.eclipse.epsilon.lsp.EpsilonTextDocumentService;
import org.eclipse.gymnast.runtime.core.parser.ParseError;
import org.eclipse.gymnast.runtime.core.parser.ParseMessage;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;

public class StandaloneEpsilonTextDocumentService extends EpsilonTextDocumentService {

	public StandaloneEpsilonTextDocumentService(StandaloneEpsilonLanguageServer languageServer) {
		super(languageServer);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		if (params.getTextDocument().getUri().endsWith(".emf")) {
			publishDiagnostics(params.getContentChanges().get(0).getText(),
				params.getTextDocument().getUri(), "emfatic");
		}
		else {
			super.didChange(params);
		}
	}

	@Override
	protected void publishDiagnostics(String code, String uri, String language) {
		if (!"emfatic".equals(language)) {
			super.publishDiagnostics(code, uri, language);
			return;
		}

		List<Diagnostic> diagnostics = new ArrayList<>();
		try {
			ResourceSet resourceSet = new ResourceSetImpl();
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("emf", new EmfaticResourceFactory());
			EmfaticResource resource = (EmfaticResource) resourceSet.createResource(
				org.eclipse.emf.common.util.URI.createURI(uri));
			resource.load(new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8)), null);
			diagnostics = getDiagnostics(resource, code);
		}
		catch (Exception ex) {
			log(ex);
		}

		List<Diagnostic> publishedDiagnostics = diagnostics;
		CompletableFuture.runAsync(() -> languageServer.getClient().publishDiagnostics(
			new PublishDiagnosticsParams(uri, publishedDiagnostics)));
	}

	protected List<Diagnostic> getDiagnostics(EmfaticResource resource, String text) {
		List<Diagnostic> diagnostics = new ArrayList<>();
		for (ParseMessage parseMessage : resource.getParseContext().getMessages()) {
			Diagnostic diagnostic = new Diagnostic();
			diagnostic.setSeverity(parseMessage instanceof ParseError
				? DiagnosticSeverity.Error : DiagnosticSeverity.Warning);
			diagnostic.setMessage(parseMessage.getMessage());
			diagnostic.setRange(new Range(
				getPosition(text, parseMessage.getOffset()),
				getPosition(text, parseMessage.getOffset() + parseMessage.getLength())));
			diagnostics.add(diagnostic);
		}
		return diagnostics;
	}
}
