package org.eclipse.epsilon.eol.dt.lsp;

import java.io.IOException;


import org.eclipse.epsilon.eol.dt.EolPlugin;
import org.eclipse.epsilon.lsp.EpsilonLanguageServer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;

public class ConnectionProviderSolution extends AbstractConnectionProvider {
    private static final EpsilonLanguageServer LANGUAGE_SERVER = new EpsilonLanguageServer();
    private static boolean preferenceListenerRegistered = false;

    public ConnectionProviderSolution() {
        super(LANGUAGE_SERVER);
        configureNativeTypeClasspath();
        registerNativeTypeClasspathPreferenceListener();
    }
    
    @Override
    public void start() throws IOException {
        super.start();
        LANGUAGE_SERVER.connect(launcher.getRemoteProxy());
    }

    private static synchronized void configureNativeTypeClasspath() {
        LANGUAGE_SERVER.setNativeTypeClasspath(getNativeTypeClasspathPreference());
    }

    private static synchronized void registerNativeTypeClasspathPreferenceListener() {
        if (preferenceListenerRegistered || EolPlugin.getDefault() == null) {
            return;
        }
        IPreferenceStore preferenceStore = EolPlugin.getDefault().getPreferenceStore();
        IPropertyChangeListener listener = event -> {
            if (LspNativeTypeClasspathPreferencePage.NATIVE_TYPE_CLASSPATH.equals(event.getProperty())) {
                LANGUAGE_SERVER.setNativeTypeClasspath(event.getNewValue() != null ? event.getNewValue().toString() : "");
                LANGUAGE_SERVER.analyser.initialize();
            }
        };
        preferenceStore.addPropertyChangeListener(listener);
        preferenceListenerRegistered = true;
    }

    private static String getNativeTypeClasspathPreference() {
        if (EolPlugin.getDefault() == null) {
            return "";
        }
        return EolPlugin.getDefault().getPreferenceStore()
            .getString(LspNativeTypeClasspathPreferencePage.NATIVE_TYPE_CLASSPATH);
    }
}
