package org.eclipse.epsilon.lsp.test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.BeforeClass;
import org.junit.Test;

public class ImportTests extends AbstractEpsilonLanguageServerTest {

	@BeforeClass
	public static void registerUrlHandler() {
		TestUrlHandlerRegistrar.registerSingletonMapHandlerOnce();
	}
	
	@Test
	public void importingFileIsRecheckedWhenLibraryChanges() throws Exception {
		// Create a temporary directory for the test files
		Path tmp = Files.createTempDirectory("eol-import-change-test");
		File lib = tmp.resolve("lib.eol").toFile();
		String libUri = lib.toPath().toAbsolutePath().toUri().toString();
		File main = tmp.resolve("main.eol").toFile();
		String mainUri = main.toPath().toAbsolutePath().toUri().toString();

		// Initial library defines x
		String libContent = "operation foo(){}";
		Files.write(lib.toPath(), libContent.getBytes(StandardCharsets.UTF_8));

		// Main imports the library and uses x
		
		String mainContent = "import 'lib.eol';" + "\nfoo();";
		Files.write(main.toPath(), mainContent.getBytes(StandardCharsets.UTF_8));

		// Put the temporary folder into the language server's workspaceFolders so
		// Analyser.initialize() will scan it.
		Field wfField = server.getClass().getDeclaredField("workspaceFolders");
		wfField.setAccessible(true);
		List<WorkspaceFolder> wf = new ArrayList<WorkspaceFolder>();
		wf.add(new WorkspaceFolder(tmp.toUri().toString(), tmp.toString()));
		wfField.set(server, wf);

		// Initialize the analyser so it processes both files and builds dependency
		// graph
		server.analyser.initialize();

		// Expect empty diagnostics for lib and main initially
		assertPublishedEmptyDiagnostics(libUri);
		assertPublishedEmptyDiagnostics(mainUri);

		
		String newLibContent = "operation bar(){}";

		// Notify the analyser of the changed library content
		server.analyser.checkChangedDocument(lib.toURI(), newLibContent);
		assertPublishedExprectedDiagnostics(mainUri, List.of("Undefined operation foo"));
	}
	
	@Test
	public void cyclicDependencies() throws Exception {
		// Create a temporary directory for the test files
		Path tmp = Files.createTempDirectory("eol-cyclic-deps-test");
		File a = tmp.resolve("a.eol").toFile();
		String aUri = a.toPath().toAbsolutePath().toUri().toString();
		File b = tmp.resolve("b.eol").toFile();
		String bUri = b.toPath().toAbsolutePath().toUri().toString();

		String aContent = "import 'b.eol';";
		Files.write(a.toPath(), aContent.getBytes(StandardCharsets.UTF_8));
	
		String bContent = "import 'a.eol';";
		Files.write(b.toPath(), bContent.getBytes(StandardCharsets.UTF_8));

		// Put the temporary folder into the language server's workspaceFolders so
		// Analyser.initialize() will scan it.
		Field wfField = server.getClass().getDeclaredField("workspaceFolders");
		wfField.setAccessible(true);
		List<WorkspaceFolder> wf = new ArrayList<WorkspaceFolder>();
		wf.add(new WorkspaceFolder(tmp.toUri().toString(), tmp.toString()));
		wfField.set(server, wf);

		// Initialize the analyser so it processes both files and builds dependency
		// graph
		server.analyser.initialize();

		// Expect empty diagnostics for lib and main initially
		assertPublishedEmptyDiagnostics(aUri);
		assertPublishedEmptyDiagnostics(bUri);
	}

}