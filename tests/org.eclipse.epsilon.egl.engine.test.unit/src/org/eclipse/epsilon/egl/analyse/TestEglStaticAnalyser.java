/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.epsilon.egl.analyse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.epsilon.common.parse.Position;
import org.eclipse.epsilon.egl.EglModule;
import org.eclipse.epsilon.eol.analyse.EolCompletion;
import org.eclipse.epsilon.eol.analyse.EolCompletionKind;
import org.junit.Test;

public class TestEglStaticAnalyser {

	@Test
	public void variableIsSuggestedInsideOutputBlock() throws Exception {
		List<EolCompletion> completions = getCompletions(
			"[% var fo = 1; %]\n[%=fo%]", new Position(2, 5));

		assertTrue(completions.stream().anyMatch(c -> "fo".equals(c.getName())));
		assertFalse(completions.stream().anyMatch(c -> "printdyn".equals(c.getName())));
	}

	@Test
	public void contextOperationIsSuggestedInsideOutputBlock() throws Exception {
		String source = "[%=\"\".foo%]\n"
			+ "[%\n"
			+ "operation String foo(x : Integer, y : Integer){}\n"
			+ "%]";
		List<EolCompletion> completions = getCompletions(source, new Position(1, 9));

		assertEquals(1, completions.size());
		assertEquals("foo", completions.get(0).getName());
		assertEquals(EolCompletionKind.OPERATION, completions.get(0).getKind());
	}

	private List<EolCompletion> getCompletions(String source, Position position) throws Exception {
		EglModule module = new EglModule();
		assertTrue(module.parse(source));

		EglStaticAnalyser analyser = new EglStaticAnalyser();
		analyser.validate(module);
		return analyser.getCompletions(module, position);
	}
}
