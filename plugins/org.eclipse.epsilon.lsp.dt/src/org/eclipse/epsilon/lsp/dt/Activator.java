/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 ******************************************************************************/
package org.eclipse.epsilon.lsp.dt;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.eclipse.epsilon.lsp.dt";
	private static final String LEGACY_PLUGIN_ID = "org.eclipse.epsilon.eol.dt";
	private static final String PREFERENCES_MIGRATED = "preferencesMigratedFromEolDt";

	private static Activator plugin;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		migratePreferences();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

	private void migratePreferences() {
		IPreferenceStore preferenceStore = getPreferenceStore();
		if (preferenceStore.getBoolean(PREFERENCES_MIGRATED)) {
			return;
		}

		// Preserve values saved before the LSP DT preferences moved out of eol.dt.
		String legacyClasspath = Platform.getPreferencesService().getString(LEGACY_PLUGIN_ID,
			LspNativeTypeClasspathPreferencePage.NATIVE_TYPE_CLASSPATH, "", null);
		if (!legacyClasspath.isEmpty()
				&& preferenceStore.getString(LspNativeTypeClasspathPreferencePage.NATIVE_TYPE_CLASSPATH).isEmpty()) {
			preferenceStore.setValue(LspNativeTypeClasspathPreferencePage.NATIVE_TYPE_CLASSPATH, legacyClasspath);
		}
		preferenceStore.setValue(PREFERENCES_MIGRATED, true);
	}
}
