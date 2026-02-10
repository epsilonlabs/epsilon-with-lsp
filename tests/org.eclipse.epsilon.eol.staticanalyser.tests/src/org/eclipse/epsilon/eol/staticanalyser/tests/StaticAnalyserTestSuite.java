package org.eclipse.epsilon.eol.staticanalyser.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	EolBuildTests.class,
	EvlStaticAnalyserTests.class
})
public class StaticAnalyserTestSuite {

}
