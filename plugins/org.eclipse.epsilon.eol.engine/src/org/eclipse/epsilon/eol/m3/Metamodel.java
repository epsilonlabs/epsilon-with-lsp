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
		if (name.contains("::")) {
			String[] parts = name.split("::");
			Package current = this;
			int startIndex = 0;
			if (current.getName() != null && current.getName().equals(parts[0])) {
				startIndex = 1;
			}
			for (int i = startIndex; i < parts.length - 1; i++) {
				Package found = null;
				for (Package sub : current.getSubPackages()) {
					if (sub.getName().equals(parts[i])) {
						found = sub;
						break;
					}
				}
				if (found == null) return null;
				current = found;
			}
			String className = parts[parts.length - 1];
			for (IMetaClass type : current.getTypes()) {
				if (type.getName().equals(className)) {
					return type;
				}
			}
			return null;
		}
		return findMetaClass(this, name);
	}
	
	private IMetaClass findMetaClass(Package pkg, String name) {
		for (IMetaClass type : pkg.getTypes()) {
			if (type.getName().equals(name)) {
				return type;
			}
		}
		for (Package sub : pkg.getSubPackages()) {
			IMetaClass result = findMetaClass(sub, name);
			if (result != null) return result;
		}
		return null;
	}
	
	public abstract boolean equals(Object other);
	
	public abstract int hashCode();
}
