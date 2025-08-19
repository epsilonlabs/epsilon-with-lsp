package org.eclipse.epsilon.eol.dt.lsp;

import java.io.IOException;


import org.eclipse.epsilon.lsp.EpsilonLanguageServer;

public class ConnectionProviderSolution extends AbstractConnectionProvider {
    private static final EpsilonLanguageServer LANGUAGE_SERVER = new EpsilonLanguageServer();
    public ConnectionProviderSolution() {
        super(LANGUAGE_SERVER);
    }
    
    @Override
    public void start() throws IOException {
        super.start();
        LANGUAGE_SERVER.connect(launcher.getRemoteProxy());
    }
}