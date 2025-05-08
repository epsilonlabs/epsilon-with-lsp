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

import java.util.Arrays;
import java.util.List;

public class EolPrimitiveType extends EolType {
	private String name;
	private Class<?> clazz;
	
	public static final EolPrimitiveType
		Integer = new EolPrimitiveType(Integer.class, "Integer"),
		String = new EolPrimitiveType(String.class, "String"),
		Boolean = new EolPrimitiveType(Boolean.class, "Boolean"),
		Real = new EolPrimitiveType(Float.class, "Real");
	
	private EolPrimitiveType(Class<?> clazz, String name) {
		this.clazz = clazz;
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public Class<?> getClazz() {
		return clazz;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public List<EolType> getParentTypes() {
		return Arrays.asList(new EolNativeType(getClazz()));
	}
}
