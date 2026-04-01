/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 ******************************************************************************/
package org.eclipse.epsilon.eol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.eol.dom.AssignmentStatement;
import org.eclipse.epsilon.eol.dom.ExpressionStatement;
import org.eclipse.epsilon.eol.dom.ForStatement;
import org.eclipse.epsilon.eol.dom.IfStatement;
import org.eclipse.epsilon.eol.dom.NameExpression;
import org.eclipse.epsilon.eol.dom.PropertyCallExpression;
import org.eclipse.epsilon.eol.dom.Statement;
import org.eclipse.epsilon.eol.dom.StatementBlock;
import org.junit.Test;

public class ParseRecoveryTests {
	
	@Test
	public void incompleteFor() throws Exception {
		EolModule module = new EolModule();
		boolean parsed = module.parse("for (i in )");

		assertFalse(parsed);
		assertNotNull(module.getMain());
		assertEquals(1, module.getMain().getStatements().size());
		ForStatement forStatement = (ForStatement) module.getMain().getStatements().get(0);
		assertNotNull(forStatement.getIteratorParameter());
		assertNull(forStatement.getIteratedExpression());
		assertFalse(module.getParseProblems().isEmpty());
		assertTrue(hasProblemContaining(module, ")"));
	}
	
	@Test
	public void incompletePropertyCallExpressionKeepsRecoveredChain() throws Exception {
		EolModule module = new EolModule();
		boolean parsed = module.parse("for (i in x.y.z.)");
		assertFalse(parsed);
		assertNotNull(module.getMain());
		assertEquals(1, module.getMain().getStatements().size());
		ForStatement forStatement = (ForStatement) module.getMain().getStatements().get(0);
		assertNotNull(forStatement.getIteratorParameter());
		assertNotNull(forStatement.getIteratedExpression());
		assertRecoveredPropertyChain(forStatement.getIteratedExpression(), "x", "y", "z");
		assertFalse(module.getParseProblems().isEmpty());
		assertTrue(hasProblemContaining(module, "NAME"));
	}
	
	@Test
	public void missingSemicolonKeepsFollowingStatementInAst() throws Exception {
		EolModule module = new EolModule();
		
		boolean parsed = module.parse("a := 1\nb := 2;");
		
		assertFalse(parsed);
		assertNotNull(module.getMain());
		assertNotEquals(0, module.getMain().getStatements().size());
		assertRecoveredAssignmentTarget(module.getMain(), "b");
		assertTrue(hasProblemContaining(module, ":="));
	}

	@Test
	public void missingClosingBraceKeepsBlockStatementsInAst() throws Exception {
		EolModule module = new EolModule();
		
		boolean parsed = module.parse("if (true) { a := 1; b := 2;");
		
		assertFalse(parsed);
		assertNotNull(module.getMain());
		assertEquals(1, module.getMain().getStatements().size());
		IfStatement ifStatement = (IfStatement) module.getMain().getStatements().get(0);
		StatementBlock thenBlock = ifStatement.getThenStatementBlock();
		assertNotNull(thenBlock);
		assertEquals(2, thenBlock.getStatements().size());
		assertAssignmentTarget(thenBlock, 0, "a");
		assertAssignmentTarget(thenBlock, 1, "b");
		assertTrue(hasProblemContaining(module, "missing"));
		assertTrue(hasProblemContaining(module, "}"));
	}

	private void assertAssignmentTarget(StatementBlock block, int statementIndex, String expectedName) {
		AssignmentStatement statement = (AssignmentStatement) block.getStatements().get(statementIndex);
		NameExpression target = (NameExpression) statement.getTargetExpression();
		assertEquals(expectedName, target.getName());
	}

	private boolean hasProblemContaining(EolModule module, String fragment) {
		for (ParseProblem problem : module.getParseProblems()) {
			if (problem.getReason() != null && problem.getReason().contains(fragment)) {
				return true;
			}
		}
		return false;
	}

	private void assertRecoveredAssignmentTarget(StatementBlock block, String expectedName) {
		for (Statement statement : block.getStatements()) {
			if (statement instanceof AssignmentStatement) {
				NameExpression target = (NameExpression) ((AssignmentStatement) statement).getTargetExpression();
				if (expectedName.equals(target.getName())) {
					return;
				}
			}
			else if (statement instanceof ExpressionStatement) {
				// Ignore placeholder expression statements created for broken syntax before the recovery point.
			}
		}
		throw new AssertionError("Expected recovered assignment to " + expectedName);
	}

	private void assertRecoveredPropertyChain(org.eclipse.epsilon.eol.dom.Expression expression, String... expectedNames) {
		List<String> actualNames = new ArrayList<>();
		org.eclipse.epsilon.eol.dom.Expression current = expression;
		while (current instanceof PropertyCallExpression) {
			PropertyCallExpression propertyCall = (PropertyCallExpression) current;
			actualNames.add(0, propertyCall.getName());
			current = propertyCall.getTargetExpression();
		}
		assertTrue(current instanceof NameExpression);
		actualNames.add(0, ((NameExpression) current).getName());
		
		assertTrue("Recovered chain should keep the complete prefix", actualNames.size() >= expectedNames.length);
		for (int i = 0; i < expectedNames.length; i++) {
			assertEquals(expectedNames[i], actualNames.get(i));
		}
		
		if (actualNames.size() > expectedNames.length) {
			assertEquals(expectedNames.length + 1, actualNames.size());
			String placeholder = actualNames.get(actualNames.size() - 1);
			assertTrue("Trailing recovered placeholder should be null or empty", placeholder == null || placeholder.isEmpty());
		}
	}
}
