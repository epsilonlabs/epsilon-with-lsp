package org.eclipse.epsilon.eol.staticanalyser.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.epsilon.common.module.ModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.staticanalyser.EolStaticAnalyser;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;
import org.junit.Before;
import org.junit.Test;

public class AbstractEOLTest extends AbstractBaseTest {

	private EOLTestMarkerStringParser testMarkerParser= new EOLTestMarkerStringParser();
	protected EolModule module;
	protected EolStaticAnalyser staticAnalyser;

	public AbstractEOLTest(String testTag, File epsilonTestFile, boolean enableconsoleoutput) {
		super(testTag, epsilonTestFile, enableconsoleoutput);
	}

	@Before
	public void setUp() {
		module = new EolModule();
		staticAnalyser = new EolStaticAnalyser(new StaticModelFactory());
	}

	@Test
	public void originalTestApproach() throws Exception {
		parseFile(programFile);
	}
	
	@Test
	public void markerMatchedTestApproach () throws Exception {
		if(isConsoleOutputActive) {
			System.out.println("\nTesting program: " + testTag);
		}
		List<ModuleMarker> testMarkers = testMarkerParser.extractTestMarkers(programFile);
		
		int regionCount = 0;
		for (ModuleMarker testMarker : testMarkers) {
			if(null != testMarker.getRegion()) {
				regionCount++;
			}
		}
		
		// No test markers in program, therefore the program should be clean
		if (testMarkers.isEmpty()) {
			if(isConsoleOutputActive) {
				System.out.println(" [!] Clean program test (no test markers)");
			}
			
			module.parse(programFile);
			List<ModuleMarker> staticAnalyserMarkers = staticAnalyser.validate(module);
			assertEquals("No test markers set, so static analysis should report nothing", 0, staticAnalyserMarkers.size());
			return;
		}

		
		if (0 == regionCount) {
			// Call assert Valid method from the old test to check the messages, these are old tests with no region info
			if(isConsoleOutputActive) {
				System.out.println(" [!] Running old test (no regions in test markers)");
			}
			List<String> errorMessages = testMarkerParser.getErrorMessageStrings(testMarkers, Severity.Error);
			List<String> warningMessages = testMarkerParser.getErrorMessageStrings(testMarkers, Severity.Warning);
			assertValid(programFile, errorMessages, warningMessages);
		}else {
			// New test must have complete region information, but we may have some missing or errors
			if(isConsoleOutputActive) {
				System.out.println(" [!] Running new test (region in atleast 1 test marker");
			}
			assertValidLineNumbered(programFile, testMarkers);
		}
	}
	
	public void assertValidLineNumbered(File programFile, List<ModuleMarker> testMarkers) throws Exception {
		module.parse(programFile);
		List<ModuleMarker> staticAnalyserMarkers = staticAnalyser.validate(module);
		
		if(isConsoleOutputActive) {
			System.out.println("  [?] Static Analyser Markers in Test Markers test");
		}
		compareLeftToRight(staticAnalyserMarkers, testMarkers, false);
		
		if(isConsoleOutputActive) {
			System.out.println("  [?] Test Markers in Static Analyser Markers test");
		}
		compareLeftToRight(testMarkers, staticAnalyserMarkers, true);
		
	}
	
	private void compareLeftToRight(List<ModuleMarker> leftListMarkers, List<ModuleMarker> rightListMarkers,
			boolean leftListIsTestMarkers) {
		boolean result = false;
		int matchCount = 0;
		
		for (ModuleMarker leftMarker : leftListMarkers) {
			result = false;
			matchCount = 0;
			for (ModuleMarker rightMarker : rightListMarkers) {
				result = compareMarkers(leftMarker, rightMarker);
				result = leftMarker.toString().equals(rightMarker.toString());
				if (result) {
					matchCount++;
				}
			}
			
			if (leftListIsTestMarkers) {
				assertEquals("Test Marker did not match 1 Static Analysis Marker: " + leftMarker,1, matchCount );
			} else {
				assertEquals("Static Analysis Marker did not match 1 Test Marker: " + leftMarker,1, matchCount );
			}
		}

	}
	
	private boolean compareMarkers(ModuleMarker first, ModuleMarker second) {
		// This could be a string compare between first and second ModuleMarker.toString(), 
		// but if the toString() method on the ModuleMarker excludes something it won't be tested.
		if (!first.getSeverity().equals(second.getSeverity())) {
			return false;
		}

		if (!first.getRegion().equals(second.getRegion())) {
			return false;
		}

		if (!first.getMessage().equals(second.getMessage())) {
			return false;
		}
		return true;
	}


	/*
	 * Methods below are from the original test, they take a program file, parse and test
	 */
	
	// Original test method
	protected void parseFile(File file) throws Exception {
		String content = new String(Files.readAllBytes(file.toPath()));
		String[] lines = content.split(System.lineSeparator());
		List<String> errorMessages = new ArrayList<String>();
		List<String> warningMessages = new ArrayList<String>();
		for (String line : lines) {
			if (!line.substring(0, 2).equals("//")) {
				break;
			}

			if (line.substring(0, 3).equals("//!")) {
				errorMessages.add(line.substring(3));
			} else if (line.substring(0, 3).equals("//?")) {
				warningMessages.add(line.substring(3));
			}
		}
		assertValid(file, errorMessages, warningMessages);
	}

	// Original test method
	public void assertValid(File file, List<String> expectedErrorMessages, List<String> expectedWarningMessages)
			throws Exception {
		module.parse(file);
		List<ModuleMarker> markers = staticAnalyser.validate(module);
		List<ModuleMarker> errors = markers.stream().filter(m -> m.getSeverity() == Severity.Error)
				.collect(Collectors.toList());
		List<ModuleMarker> warnings = markers.stream().filter(m -> m.getSeverity() == Severity.Warning)
				.collect(Collectors.toList());

		String errorMessages = errors.stream()
				.map((e) -> e.getMessage() + " line: " + e.getRegion().getStart().getLine())
				.collect(Collectors.joining("\n"));
		assertEquals("Unexpected number of errors\n" + errorMessages + "\n", expectedErrorMessages.size(),
				errors.size());

		Map<String, Integer> errorMessagesMap = new HashMap<String, Integer>();
		for (String errorMessage : errors.stream().map(ModuleMarker::getMessage).collect(Collectors.toList())) {
			if (errorMessagesMap.containsKey(errorMessage)) {
				errorMessagesMap.put(errorMessage, errorMessagesMap.get(errorMessage) + 1);
			} else {
				errorMessagesMap.put(errorMessage, 1);
			}
		}
		for (String errorMessage : expectedErrorMessages) {
			assertTrue("An expected error was not found in the list of thrown errors",
					errorMessagesMap.containsKey(errorMessage));
			errorMessagesMap.put(errorMessage, errorMessagesMap.get(errorMessage) - 1);
		}
		for (Integer i : errorMessagesMap.values()) {
			assertFalse("An error message was not matched enough times", i > 0);
			assertFalse("An error message was matched too many times", i < 0);
		}

		String warningMessages = warnings.stream()
				.map((e) -> e.getMessage() + " line: " + e.getRegion().getStart().getLine())
				.collect(Collectors.joining("\n"));
		assertEquals("Unexpected number of warnings\n" + warningMessages + "\n", expectedWarningMessages.size(),
				warnings.size());

		Map<String, Integer> warningMessagesMap = new HashMap<String, Integer>();
		for (String warningMessage : warnings.stream().map(ModuleMarker::getMessage).collect(Collectors.toList())) {
			if (warningMessagesMap.containsKey(warningMessage)) {
				warningMessagesMap.put(warningMessage, warningMessagesMap.get(warningMessage) + 1);
			} else {
				warningMessagesMap.put(warningMessage, 1);
			}
		}
		for (String warningMessage : expectedWarningMessages) {
			assertTrue("An expected warning was not found in the list of thrown warnings",
					warningMessagesMap.containsKey(warningMessage));
			warningMessagesMap.put(warningMessage, warningMessagesMap.get(warningMessage) - 1);
		}
		for (Integer i : warningMessagesMap.values()) {
			assertFalse("A warning message was not matched enough times", i > 0);
			assertFalse("A warning message was matched too many times", i < 0);
		}

		visit(module.getChildren());  // Abstract syntax tree type test
	}

	// Original test method -- Checks the Abstract syntax tree types against a multiline comment /* */ for each type
	protected void visit(List<ModuleElement> elements) {
		for (ModuleElement element : elements) {
			// Multiline comments (/* */") are used to capture the expected type of expressions
			if (!element.getComments().isEmpty() && element.getComments().get(0).isMultiline()) {
				assertEquals(element.getComments().get(0).toString(), getResolvedType(element).toString());
			}
			visit(element.getChildren());
		}
	}

	// Original test method
	protected EolType getResolvedType(ModuleElement element) {
		return (EolType) element.getData().get("resolvedType");
	}

}
