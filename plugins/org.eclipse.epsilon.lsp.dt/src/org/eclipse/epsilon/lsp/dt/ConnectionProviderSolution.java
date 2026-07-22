package org.eclipse.epsilon.lsp.dt;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.Platform;
import org.eclipse.epsilon.common.dt.launching.extensions.ToolExtension;
import org.eclipse.epsilon.lsp.EpsilonLanguageServer;
import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.osgi.framework.Bundle;

public class ConnectionProviderSolution implements StreamConnectionProvider {

	private static final EpsilonLanguageServer LANGUAGE_SERVER = createLanguageServer();

	private InputStream clientInputStream;
	private OutputStream clientOutputStream;
	private Launcher<LanguageClient> launcher;
	private InputStream errorStream;
	private Future<Void> listener;
	private Collection<Closeable> streams = new ArrayList<>(4);

	private static EpsilonLanguageServer createLanguageServer() {
		EpsilonLanguageServer languageServer = new EpsilonLanguageServer();
		languageServer.setModelFactory(new ExtensionBasedModelFactory());
		languageServer.addNativeTypeClassLoader(new ExtensionPointToolClassLoader());
		return languageServer;
	}

	@Override
	public void start() throws IOException {
		Pipe serverOutputToClientInput = Pipe.open();
		Pipe clientOutputToServerInput = Pipe.open();

		errorStream = new ByteArrayInputStream("Error output on console".getBytes(StandardCharsets.UTF_8));
		InputStream serverInputStream = Channels.newInputStream(clientOutputToServerInput.source());
		OutputStream serverOutputStream = Channels.newOutputStream(serverOutputToClientInput.sink());
		launcher = LSPLauncher.createServerLauncher(LANGUAGE_SERVER, serverInputStream, serverOutputStream);
		clientInputStream = Channels.newInputStream(serverOutputToClientInput.source());
		clientOutputStream = Channels.newOutputStream(clientOutputToServerInput.sink());
		listener = launcher.startListening();
		streams.add(clientInputStream);
		streams.add(clientOutputStream);
		streams.add(serverInputStream);
		streams.add(serverOutputStream);
		streams.add(errorStream);
		LANGUAGE_SERVER.connect(launcher.getRemoteProxy());
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
		streams.forEach(stream -> {
			try {
				stream.close();
			}
			catch (IOException e) {
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

	private static class ExtensionPointToolClassLoader extends ClassLoader {
		public ExtensionPointToolClassLoader() {
			super(null);
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			ToolExtension extension = ToolExtension.forClass(name);
			if (extension == null) {
				throw new ClassNotFoundException(name);
			}

			Bundle bundle = Platform.getBundle(extension.getConfigurationElement().getContributor().getName());
			if (bundle == null) {
				throw new ClassNotFoundException(name);
			}

			return bundle.loadClass(extension.getClazz());
		}
	}
}
