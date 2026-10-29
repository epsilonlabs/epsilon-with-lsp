/*******************************************************************************
 * Copyright (c) 2008 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Louis Rose - initial API and implementation
 ******************************************************************************/
package org.eclipse.epsilon.egl;

import static org.eclipse.epsilon.egl.util.FileUtil.NEWLINE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import org.eclipse.epsilon.eol.IEolModule;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTemplateFactoryModuleAdapter {

	private static File VALID_PATH, INVALID_PATH, INVALID_RUNTIME_PATH, INVALID_EGL_PATH;	
	private final static String
		valid          = "[% for (i in Sequence{1..10}) { %]i is [%=i+2%]" + NEWLINE + "[% } %]",
		invalid        = "[%  fr (i in Sequence{1..10}) { %]i is [%=i+2%]" + NEWLINE + "[% } %]",
		invalidEgl     = "[% for (i in Sequence{1..10}) { %]i is [%=i+2";
	
	private IEolModule module;
	
	
	@BeforeClass
	public static void setUpOnce() throws IOException {	
		VALID_PATH           = org.eclipse.epsilon.common.util.FileUtil.createTempFile("Valid", ".txt");
		INVALID_PATH         = org.eclipse.epsilon.common.util.FileUtil.createTempFile("Invalid.", "t.xt");
		INVALID_RUNTIME_PATH = org.eclipse.epsilon.common.util.FileUtil.createTempFile("InvalidRuntime", ".txt");
		INVALID_EGL_PATH     = org.eclipse.epsilon.common.util.FileUtil.createTempFile("InvalidEgl", ".txt");
		
		org.eclipse.epsilon.egl.util.FileUtil.write(VALID_PATH,           valid);
		org.eclipse.epsilon.egl.util.FileUtil.write(INVALID_PATH,         invalid);
		org.eclipse.epsilon.egl.util.FileUtil.write(INVALID_RUNTIME_PATH, valid);
		org.eclipse.epsilon.egl.util.FileUtil.write(INVALID_EGL_PATH,     invalidEgl);
	}
	
	@AfterClass
	public static void tearDownOnce() {
		if (VALID_PATH.exists())           VALID_PATH.delete();
		if (INVALID_PATH.exists())         INVALID_PATH.delete();
		if (INVALID_RUNTIME_PATH.exists()) INVALID_RUNTIME_PATH.delete();
		if (INVALID_EGL_PATH.exists())     INVALID_EGL_PATH.delete();
		
	}
	
	@Before
	public void setUp() {
		module = new EglModule(new EglTemplateFactory());
	}
	
	@Test
	public void parseValidText() throws Exception {
		assertTrue(module.parse(valid));
	}
	
	@Test
	public void parseInvalidText() throws Exception {
		assertFalse(module.parse(invalid));
	}
	
	@Test
	public void parseInvalidRuntimeText() throws Exception {
		assertTrue(module.parse(valid));
	}
	
	@Test
	public void parseValidFile() throws Exception {
		assertTrue(module.parse(VALID_PATH));
	}
	
	@Test
	public void parseInvalidFile() throws Exception {
		assertFalse(module.parse(INVALID_PATH));
	}
	
	@Test
	public void parseInvalidRuntimeFile() throws Exception {
		assertTrue(module.parse(INVALID_RUNTIME_PATH));
	}
	
	@Test
	public void parseInvalidEglFile() throws Exception {
		assertFalse(module.parse(INVALID_EGL_PATH));
	}
	
	@Test
	public void uriIsSetWhenParsingFails() throws Exception {
		module.parse(INVALID_PATH);
		assertEquals(INVALID_PATH.toURI(), module.getUri());
		
		module = new EglModule(new EglTemplateFactory());
		module.parse(INVALID_EGL_PATH.toURI());
		assertEquals(INVALID_EGL_PATH.toURI(), module.getUri());
		assertEquals(INVALID_EGL_PATH.toURI(), module.getSourceUri());
	}
}
