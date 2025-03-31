/*********************************************************************
* Copyright (c) 2008 The University of York.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.epsilon.eol.m3;

import java.util.ArrayList;
import java.util.List;

public abstract class Metamodel extends Package implements IMetamodel{

	protected List<String> warnings = new ArrayList<>();
	protected List<String> errors = new ArrayList<>();

	public List<String> getWarnings() {
		return warnings;
	}
	
	public List<String> getErrors() {
		return errors;
	}
	
	public IMetaClass getMetaClass(String name) {
		for (MetaType type : metaTypes) {
			if (type instanceof IMetaClass && type.getName().equals(name)) {
				return (IMetaClass) type;
			}
		}
		return null;
	}
	
	public abstract boolean equals(Object other);
	
	public abstract int hashCode();
}
