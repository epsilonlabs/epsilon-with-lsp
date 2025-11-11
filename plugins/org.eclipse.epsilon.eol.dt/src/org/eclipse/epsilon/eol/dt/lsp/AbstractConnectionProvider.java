package org.eclipse.epsilon.eol.dt.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;
import java.io.ByteArrayInputStream;
import java.io.Closeable;

import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

public class AbstractConnectionProvider implements StreamConnectionProvider {

	private InputStream clientInputStream  ;
	private OutputStream clientOutputStream;
    private LanguageServer languageServer;
    protected Launcher<LanguageClient> launcher;
    private InputStream errorStream;
	private Future<Void> listener;
	private Collection<Closeable> streams = new ArrayList<>(4);

    public AbstractConnectionProvider(LanguageServer languageServer) {
        this.languageServer = languageServer;
    }

    @Override
    public void start() throws IOException {
		Pipe serverOutputToClientInput = Pipe.open();
		Pipe clientOutputToServerInput = Pipe.open();

		errorStream = new ByteArrayInputStream("Error output on console".getBytes(StandardCharsets.UTF_8));
		InputStream serverInputStream = Channels.newInputStream(clientOutputToServerInput.source());
		OutputStream serverOutputStream = Channels.newOutputStream(serverOutputToClientInput.sink());
		launcher = LSPLauncher.createServerLauncher(languageServer, serverInputStream,
				serverOutputStream);
		clientInputStream = Channels.newInputStream(serverOutputToClientInput.source());
		clientOutputStream = Channels.newOutputStream(clientOutputToServerInput.sink());
		listener = launcher.startListening();
		streams.add(clientInputStream);
		streams.add(clientOutputStream);
		streams.add(serverInputStream);
		streams.add(serverOutputStream);
		streams.add(errorStream);
		
    }

    @Override
    public InputStream getInputStream() {
    	return clientInputStream;
    }

    @Override
    public OutputStream getOutputStream() {
		return clientOutputStream;
    }

    @Override
    public void stop() {
		streams.forEach(t -> {
			try {
				t.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		streams.clear();
		listener.cancel(true);
		listener = null;
    }

    @Override
    public InputStream getErrorStream() {
        return errorStream;
    }
}