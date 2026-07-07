package org.eclipse.epsilon.lsp.dt;

import java.io.IOException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.epsilon.common.dt.launching.extensions.ToolExtension;
import org.eclipse.epsilon.lsp.EpsilonLanguageServer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.osgi.framework.Bundle;

public class ConnectionProviderSolution extends AbstractConnectionProvider {

	private static final EpsilonLanguageServer LANGUAGE_SERVER = createLanguageServer();
	private static boolean preferenceListenerRegistered = false;

	private static EpsilonLanguageServer createLanguageServer() {
		EpsilonLanguageServer languageServer = new EpsilonLanguageServer();
		languageServer.addNativeTypeClassLoader(new ExtensionPointToolClassLoader());
		return languageServer;
	}

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
		IPreferenceStore preferenceStore = getPreferenceStore();
		if (preferenceListenerRegistered || preferenceStore == null) {
			return;
		}

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
		IPreferenceStore preferenceStore = getPreferenceStore();
		return preferenceStore != null
			? preferenceStore.getString(LspNativeTypeClasspathPreferencePage.NATIVE_TYPE_CLASSPATH)
			: "";
	}

	private static IPreferenceStore getPreferenceStore() {
		return Activator.getDefault() != null ? Activator.getDefault().getPreferenceStore() : null;
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
