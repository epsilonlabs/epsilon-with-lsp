/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 ******************************************************************************/
package org.eclipse.epsilon.eol.dt.lsp;

import org.eclipse.epsilon.eol.dt.EolPlugin;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class LspNativeTypeClasspathPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String NATIVE_TYPE_CLASSPATH = "lspNativeTypeClasspath";

	private Text classpathText;

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		Label description = new Label(composite, SWT.WRAP);
		description.setText("Classpath entries used by the Epsilon LSP static analyser when resolving Native(\"...\") types. "
			+ "Use one entry per line. Entries may be absolute, workspace-relative, directories, jars, or simple globs such as lib/*.jar.");
		description.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		classpathText = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData classpathTextData = new GridData(SWT.FILL, SWT.FILL, true, true);
		classpathTextData.heightHint = 160;
		classpathText.setLayoutData(classpathTextData);
		classpathText.setText(EolPlugin.getDefault().getPreferenceStore().getString(NATIVE_TYPE_CLASSPATH));

		return composite;
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		EolPlugin.getDefault().getPreferenceStore().setValue(NATIVE_TYPE_CLASSPATH, classpathText.getText());
		return true;
	}

	@Override
	protected void performDefaults() {
		classpathText.setText(EolPlugin.getDefault().getPreferenceStore().getDefaultString(NATIVE_TYPE_CLASSPATH));
		super.performDefaults();
	}

}
