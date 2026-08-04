package org.eclipse.epsilon.lsp;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.services.WorkspaceService;

public class EpsilonWorkspaceService implements WorkspaceService {

	public static final String OPEN_DOCUMENT_SYMBOL_COMMAND = "epsilon.openDocumentSymbol";
	private static final Gson GSON = new Gson();
	private static final AtomicLong NAVIGATION_REQUEST_SEQUENCE = new AtomicLong();
	private final AtomicLong latestNavigationRequest = new AtomicLong();
    
    protected EpsilonLanguageServer languageServer;

    public EpsilonWorkspaceService(EpsilonLanguageServer languageServer) {
        this.languageServer = languageServer;
    }
    
    @Override
    public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    }

	public static ExecuteCommandParams createOpenDocumentSymbolCommand(
			URI documentUri, DocumentSymbol root, List<Integer> childPath) {
		Position rootPosition = root.getRange().getStart();
		List<String> pathSignatures = new ArrayList<>(childPath.size() + 1);
		List<List<String>> siblingSignatures = new ArrayList<>(childPath.size());
		DocumentSymbol selected = root;
		pathSignatures.add(documentSymbolSignature(selected));
		for (Integer childIndex : childPath) {
			List<DocumentSymbol> children = selected.getChildren();
			if (childIndex == null || childIndex < 0 || children == null || childIndex >= children.size()) {
				throw new IllegalArgumentException("Invalid document symbol child path");
			}
			List<String> signatures = new ArrayList<>(children.size());
			for (DocumentSymbol child : children) {
				signatures.add(documentSymbolSignature(child));
			}
			siblingSignatures.add(signatures);
			selected = children.get(childIndex);
			pathSignatures.add(documentSymbolSignature(selected));
		}

		List<Object> arguments = new ArrayList<>(7);
		arguments.add(documentUri.toString());
		arguments.add(rootPosition.getLine());
		arguments.add(rootPosition.getCharacter());
		arguments.add(pathSignatures);
		arguments.add(new ArrayList<>(childPath));
		arguments.add(siblingSignatures);
		arguments.add(NAVIGATION_REQUEST_SEQUENCE.incrementAndGet());
		return new ExecuteCommandParams(OPEN_DOCUMENT_SYMBOL_COMMAND, arguments);
	}

	static String documentSymbolSignature(DocumentSymbol symbol) {
		JsonArray signature = new JsonArray();
		signature.add(symbol.getName());
		signature.add(symbol.getDetail());
		addRange(signature, symbol.getRange());
		addRange(signature, symbol.getSelectionRange());
		return signature.toString();
	}

	private static void addRange(JsonArray signature, Range range) {
		if (range == null || range.getStart() == null || range.getEnd() == null) {
			signature.add(-1);
			signature.add(-1);
			signature.add(-1);
			signature.add(-1);
			return;
		}
		signature.add(range.getStart().getLine());
		signature.add(range.getStart().getCharacter());
		signature.add(range.getEnd().getLine());
		signature.add(range.getEnd().getCharacter());
	}

	@Override
	public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
		if (!OPEN_DOCUMENT_SYMBOL_COMMAND.equals(params.getCommand())) {
			return CompletableFuture.completedFuture(null);
		}

		try {
			List<Object> arguments = params.getArguments();
			if (arguments == null || arguments.size() != 7) {
				return CompletableFuture.completedFuture(null);
			}

			URI documentUri = URI.create(json(arguments.get(0)).getAsString());
			int rootLine = json(arguments.get(1)).getAsInt();
			int rootCharacter = json(arguments.get(2)).getAsInt();
			JsonArray signaturesArgument = json(arguments.get(3)).getAsJsonArray();
			List<String> pathSignatures = new ArrayList<>(signaturesArgument.size());
			for (JsonElement signatureElement : signaturesArgument) {
				pathSignatures.add(signatureElement.getAsString());
			}
			JsonArray pathArgument = json(arguments.get(4)).getAsJsonArray();
			List<Integer> childPath = new ArrayList<>(pathArgument.size());
			for (JsonElement pathElement : pathArgument) {
				childPath.add(pathElement.getAsInt());
			}
			JsonArray siblingsArgument = json(arguments.get(5)).getAsJsonArray();
			List<List<String>> siblingSignatures = new ArrayList<>(siblingsArgument.size());
			for (JsonElement siblingsElement : siblingsArgument) {
				JsonArray siblingArray = siblingsElement.getAsJsonArray();
				List<String> signatures = new ArrayList<>(siblingArray.size());
				for (JsonElement signatureElement : siblingArray) {
					signatures.add(signatureElement.getAsString());
				}
				siblingSignatures.add(signatures);
			}
			long requestId = json(arguments.get(6)).getAsLong();
			if (latestNavigationRequest.accumulateAndGet(requestId, Math::max) != requestId) {
				return CompletableFuture.completedFuture(null);
			}

			Location location = languageServer.analyser.getDocumentSymbolLocation(
				documentUri, rootLine, rootCharacter, pathSignatures, childPath, siblingSignatures);
			if (location == null || latestNavigationRequest.get() != requestId) {
				return CompletableFuture.completedFuture(null);
			}
			return CompletableFuture.completedFuture(location);
		}
		catch (RuntimeException ex) {
			return CompletableFuture.completedFuture(null);
		}
	}

	private static JsonElement json(Object value) {
		return value instanceof JsonElement ? (JsonElement) value : GSON.toJsonTree(value);
	}

	public static Location toDocumentSymbolLocation(Object value) {
		try {
			return value instanceof Location ? (Location) value : GSON.fromJson(json(value), Location.class);
		}
		catch (RuntimeException ex) {
			return null;
		}
	}
    
}
