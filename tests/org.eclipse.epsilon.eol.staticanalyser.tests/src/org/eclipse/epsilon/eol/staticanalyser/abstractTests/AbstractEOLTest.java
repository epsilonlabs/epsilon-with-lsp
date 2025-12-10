package org.eclipse.epsilon.eol.staticanalyser.abstractTests;

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
import org.eclipse.epsilon.eol.staticanalyser.tests.StaticModelFactory;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;
import org.junit.Before;
import org.junit.Test;

public class AbstractEOLTest extends AbstractBaseTest{
		
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
	public void testFileParsing() throws Exception {
		System.out.println("\nEOL Testing: " + testTag + "\n - path: "  + programFolder + programFile);
		parseFile(programFile);
	}
	
	// Format for test files  //!startline:startcolumn-endline:endcolumn message
	protected ModuleMarker createTestMarker(String testMarkedProgramLine) {
		// Just in case...
		if (!testMarkedProgramLine.substring(0,2).equals("//")){
			return null;
		}
		
		ModuleMarker testMarker = new ModuleMarker();
			// Extract Severity level 
			String severityString = testMarkedProgramLine.substring(0,3);	
			switch (severityString) {
			case "//!": // ERROR
				testMarker.setSeverity(Severity.Error);
				break;
			case "//?": // WARNING
				testMarker.setSeverity(Severity.Warning);
				break;
			default:
				// TODO handle unknown severity markup
				// Something is borked
				throw new RuntimeException("Unknown severtiy string: " + severityString);
			}
		
			// Column and line information, message
			if ('[' != testMarkedProgramLine.charAt(0)) {
				// original markup no column or line checks
				testMarker.setMessage(testMarkedProgramLine.substring(3));
				return testMarker;
			} else {
				// New markup
			}

		return testMarker;		
	}

	protected void parseFile(File file) throws Exception {
		String content = new String(Files.readAllBytes(file.toPath()));
		String[] lines = content.split(System.lineSeparator());
		List<String> errorMessages = new ArrayList<String>();
		List<String> warningMessages = new ArrayList<String>();
		
		List <ModuleMarker> testMarkerList= new ArrayList<ModuleMarker>();
		for (String line: lines) {
			if (!line.substring(0,2).equals("//")){
				break;
			} else {
				ModuleMarker testLineMarker = createTestMarker(line);
				if(null != testLineMarker) {
					testMarkerList.add(testLineMarker);
				}
			}
		
			if (line.substring(0,3).equals("//!")) {
				errorMessages.add(line.substring(3));
			}
			else if (line.substring(0,3).equals("//?")){
				warningMessages.add(line.substring(3));
			}
		}
		assertValid(file, errorMessages, warningMessages);
	}

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

		visit(module.getChildren());
	}

	protected void visit(List<ModuleElement> elements) {
		for (ModuleElement element : elements) {
			// Multiline comments are used to capture the expected type of expressions
			if (!element.getComments().isEmpty() && element.getComments().get(0).isMultiline()) {
				assertEquals(element.getComments().get(0).toString(), getResolvedType(element).toString());
			}
			visit(element.getChildren());
		}
	}

	protected EolType getResolvedType(ModuleElement element) {
		return (EolType) element.getData().get("resolvedType");
	}
	
}
