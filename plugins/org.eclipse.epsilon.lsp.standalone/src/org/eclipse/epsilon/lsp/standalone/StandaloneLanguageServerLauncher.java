/*******************************************************************************
 * Copyright (c) 2024 The University of York.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.epsilon.lsp.standalone;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;

/** TCP launcher for the standalone Epsilon language server. */
public class StandaloneLanguageServerLauncher implements Runnable {

	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 1234;

	private static final Logger LOGGER =
		Logger.getLogger(StandaloneLanguageServerLauncher.class.getName());

	private final String host;
	private final int port;
	private final CompletableFuture<Boolean> started = new CompletableFuture<>();
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final List<Future<Void>> listeningLaunchers = new ArrayList<>();
	private final StandaloneEpsilonLanguageServer languageServer;

	private ServerSocket serverSocket;
	private ExecutorService executorService;

	public StandaloneLanguageServerLauncher(
			StandaloneEpsilonLanguageServer server, String host, int port) {
		if (server == null) {
			throw new IllegalArgumentException("Language server must not be null");
		}
		languageServer = server;
		this.host = host;
		this.port = port;
	}

	public static void main(String[] args) {
		String host = DEFAULT_HOST;
		int port = DEFAULT_PORT;

		for (int i = 0; i < args.length; i++) {
			if ("--help".equals(args[i]) || "-h".equals(args[i])) {
				printUsage();
				return;
			}
			if ("--host".equals(args[i]) && i + 1 < args.length) {
				host = args[++i];
			}
			else if ("--port".equals(args[i]) && i + 1 < args.length) {
				port = Integer.parseInt(args[++i]);
			}
			else {
				throw new IllegalArgumentException("Unknown or incomplete argument: " + args[i]);
			}
		}

		StandaloneEpsilonLanguageServer server = new StandaloneEpsilonLanguageServer();
		server.setExitFunction(System::exit);
		new StandaloneLanguageServerLauncher(server, host, port).run();
	}

	private static void printUsage() {
		System.out.println("Usage: java -jar language-server.jar [--host HOST] [--port PORT]");
	}

	@Override
	public void run() {
		try {
			if (running.compareAndSet(false, true)) {
				serverSocket = new ServerSocket(port, 0, InetAddress.getByName(host));
				started.complete(true);
				LOGGER.info(() -> String.format("Started Epsilon language server on %s:%d",
					serverSocket.getInetAddress().getHostName(), serverSocket.getLocalPort()));
			}
			else {
				throw new IllegalStateException("Server has already been started");
			}
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE,
				String.format("Failed to start server on %s:%d", host, port), ex);
			running.set(false);
			started.complete(false);
			return;
		}

		try {
			while (running.get()) {
				Socket connection = serverSocket.accept();
				if (executorService == null) {
					executorService = Executors.newCachedThreadPool();
				}

				Launcher<LanguageClient> launcher = Launcher.createLauncher(
					languageServer,
					LanguageClient.class,
					connection.getInputStream(),
					connection.getOutputStream(),
					executorService,
					null);
				languageServer.connect(launcher.getRemoteProxy());
				synchronized (listeningLaunchers) {
					listeningLaunchers.add(launcher.startListening());
				}
			}
		}
		catch (SocketException ex) {
			LOGGER.log(Level.FINEST, ex.getMessage(), ex);
		}
		catch (IOException ex) {
			throw new RuntimeException("Error during execution of language server", ex);
		}
		finally {
			shutdown();
		}
	}

	public Future<Boolean> isStarted() {
		return started;
	}

	public void shutdown() {
		if (!running.compareAndSet(true, false)) {
			return;
		}

		synchronized (listeningLaunchers) {
			listeningLaunchers.forEach(launcher -> launcher.cancel(true));
			listeningLaunchers.clear();
		}

		if (executorService != null) {
			executorService.shutdown();
			try {
				executorService.awaitTermination(10, TimeUnit.SECONDS);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				LOGGER.log(Level.WARNING, ex.getMessage(), ex);
			}
			executorService = null;
		}

		if (serverSocket != null) {
			try {
				serverSocket.close();
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, "Error while shutting down language server", ex);
			}
			finally {
				serverSocket = null;
			}
		}
	}

	public String getHost() {
		return running.get() ? serverSocket.getInetAddress().getHostName() : host;
	}

	public int getPort() {
		return running.get() ? serverSocket.getLocalPort() : port;
	}
}
