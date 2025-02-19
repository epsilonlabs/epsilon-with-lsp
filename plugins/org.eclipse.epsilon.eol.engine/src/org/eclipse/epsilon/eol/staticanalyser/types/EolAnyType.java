/*******************************************************************************
 * Copyright (c) 2008 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Dimitrios Kolovos - initial API and implementation
 ******************************************************************************/
package org.eclipse.epsilon.eol.staticanalyser.types;

public class EolAnyType extends EolType {

	public static final EolAnyType Instance = new EolAnyType();
	
	@Override
	public boolean isNot(EolType type) {
		return false;
	}
	
	@Override
	public String getName() {
		return "Any";
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	protected EolType getParentType() {
		return null;
	}
	
	@Override
	public boolean isAncestorOf(EolType type) {
		return false;
	}
}
