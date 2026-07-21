package org.eclipse.epsilon.lsp.standalone.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;

@RunWith(Suite.class)
@SuiteClasses(StandaloneLanguageServerTest.class)
public class StandaloneLanguageServerTestSuite {
	public static Test suite() {
		return new JUnit4TestAdapter(StandaloneLanguageServerTestSuite.class);
	}
}
