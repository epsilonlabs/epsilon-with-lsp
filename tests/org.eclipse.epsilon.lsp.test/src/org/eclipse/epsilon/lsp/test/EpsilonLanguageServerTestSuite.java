package org.eclipse.epsilon.lsp.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;

@RunWith(Suite.class)
@SuiteClasses({
	SyntaxCheckTest.class,
	ImportTests.class,
	GlobalPackageRegistryTest.class
})
public class EpsilonLanguageServerTestSuite {
	public static Test suite() {
		return new JUnit4TestAdapter(EpsilonLanguageServerTestSuite.class);
	}
}
