/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.epsilon.lsp.dt.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.Test;

public class SnippetCompletionTest {

	@Test
	public void startupHookIsLoadedFromPdeOutput() throws Exception {
		Class<?> hook = Platform.getBundle("org.eclipse.epsilon.lsp.dt")
			.loadClass("org.eclipse.epsilon.lsp.dt.SnippetCompletionSupport");
		Path loaded = Path.of(FileLocator.toFileURL(hook.getResource("SnippetCompletionSupport.class")).toURI());
		Path expected = Path.of(System.getProperty("epsilon.lsp.dt.output"),
			"org/eclipse/epsilon/lsp/dt/SnippetCompletionSupport.class");
		assertEquals("The editor hook must be present in the PDE launch output", expected.toRealPath(), loaded.toRealPath());
	}

	@Test
	public void typingArgumentsKeepsTabNavigationWithoutOpeningCompletion() throws Exception {
		var preferences = InstanceScope.INSTANCE.getNode("org.eclipse.ui.genericeditor");
		preferences.putBoolean("contentAssistant.autoActivation", true);
		preferences.putBoolean("contentAssistant.autoActivationOnType", true);
		preferences.putInt("contentAssistant.autoActivationDelay", 10);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("snippet-completion");
		project.create(null);
		project.open(null);
		ITextEditor editor = null;
		try {
			String source = "'test'.fo;\noperation String foo(x:Integer, y:Integer) {}\n";
			IFile file = project.getFile("snippet.eol");
			file.create(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), true, null);
			editor = (ITextEditor) IDE.openEditor(page, file, "org.eclipse.ui.genericeditor.GenericEditor");
			ITextViewer viewer = (ITextViewer) editor.getAdapter(ITextOperationTarget.class);
			viewer.setSelectedRange(source.indexOf(';'), 0);
			viewer.getTextWidget().setFocus();
			pump(500);
			viewer.getTextOperationTarget().doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
			await(() -> completionTable() != null && selectFoo(completionTable()));
			verifyKey(viewer, SWT.CR);
			pump(300);
			assertEquals("Document after Enter: " + viewer.getDocument().get(), "x", selectedText(viewer));
			assertNoCompletionPopup();

			key('5', '5');
			pump(300);
			assertTrue(viewer.getDocument().get().startsWith("'test'.foo(5, y);"));
			assertTrue(LinkedModeModel.hasInstalledModel(viewer.getDocument()));
			assertNoCompletionPopup();
			key('\t', SWT.TAB);
			await(() -> "y".equals(selectedText(viewer)));
			assertNoCompletionPopup();

			// Explicit completion is still available inside a placeholder.
			viewer.getTextOperationTarget().doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
			await(() -> completionTable() != null);
			verifyKey(viewer, SWT.ESC);
			pump(100);
			assertNoCompletionPopup();
			verifyKey(viewer, SWT.ESC);
			await(() -> !LinkedModeModel.hasInstalledModel(viewer.getDocument()));
			String activationSource = source.replace("'test'.fo;", "'test'.f;");
			viewer.getDocument().set(activationSource);
			viewer.setSelectedRange(activationSource.indexOf(';'), 0);
			// Linked-mode exit restores activation asynchronously. Drain that work and
			// regain editor focus before posting the next native key event (notably on Cocoa).
			boolean[] readyForTyping = { false };
			Display.getCurrent().asyncExec(() -> {
				viewer.getTextWidget().setFocus();
				readyForTyping[0] = true;
			});
			await(() -> readyForTyping[0] && viewer.getTextWidget().isFocusControl());
			key('o', 'o');
			await(() -> viewer.getDocument().get().startsWith("'test'.fo;"));
			await(() -> completionTable() != null && selectFoo(completionTable()));
		}
		finally {
			if (editor != null) {
				page.closeEditor(editor, false);
			}
			project.delete(true, true, null);
		}
	}

	private static String selectedText(ITextViewer viewer) {
		return viewer.getTextWidget().getSelectionText();
	}

	private static boolean selectFoo(Table table) {
		for (int index = 0; index < table.getItemCount(); index++) {
			if (table.getItem(index).getText().startsWith("foo(")) {
				table.setSelection(index);
				table.notifyListeners(SWT.Selection, new Event());
				return true;
			}
		}
		return false;
	}

	private static void assertNoCompletionPopup() {
		Table table = completionTable();
		StringBuilder proposals = new StringBuilder();
		if (table != null) {
			for (TableItem item : table.getItems()) {
				proposals.append(item.getText()).append(' ');
			}
		}
		assertFalse("Unexpected completion dropdown: " + proposals, table != null);
	}

	private static Table completionTable() {
		for (Shell shell : Display.getCurrent().getShells()) {
			if (shell.isVisible() && (shell.getStyle() & SWT.ON_TOP) != 0) {
				Table table = findTable(shell);
				if (table != null && table.getItemCount() > 0) {
					return table;
				}
			}
		}
		return null;
	}

	private static Table findTable(Composite parent) {
		for (Control control : parent.getChildren()) {
			if (control instanceof Table) {
				return (Table) control;
			}
			if (control instanceof Composite) {
				Table table = findTable((Composite) control);
				if (table != null) return table;
			}
		}
		return null;
	}

	private static void key(char character, int keyCode) {
		Event event = new Event();
		event.type = SWT.KeyDown;
		event.character = character;
		event.keyCode = keyCode;
		assertTrue(Display.getCurrent().post(event));
		event.type = SWT.KeyUp;
		assertTrue(Display.getCurrent().post(event));
	}

	private static void verifyKey(ITextViewer viewer, char character) {
		Event event = new Event();
		event.character = character;
		event.keyCode = character;
		event.doit = true;
		viewer.getTextWidget().notifyListeners(ST.VerifyKey, event);
	}

	private static void await(BooleanSupplier condition) throws InterruptedException {
		long deadline = System.nanoTime() + 10_000_000_000L;
		while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
			pump(10);
		}
		assertTrue("Timed out waiting for editor state", condition.getAsBoolean());
	}

	private static void pump(long milliseconds) throws InterruptedException {
		long deadline = System.nanoTime() + milliseconds * 1_000_000L;
		do {
			while (Display.getCurrent().readAndDispatch() && System.nanoTime() < deadline) {
				// Process real key events, asynchronous completions and activation timers.
			}
			Thread.sleep(5);
		} while (System.nanoTime() < deadline);
	}
}
