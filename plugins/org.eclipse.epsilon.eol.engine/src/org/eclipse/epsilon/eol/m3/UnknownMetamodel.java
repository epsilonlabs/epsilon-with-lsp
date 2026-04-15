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
import java.util.Collections;
import java.util.List;

/**
 * A metamodel for the Unknown EMC driver. Dynamically accepts any type
 * name that starts with an uppercase letter, treating it as a valid
 * model element type. This allows static analysis to proceed with
 * partial type information when the actual metamodel is not available.
 */
public class UnknownMetamodel extends Metamodel {

	@Override
	public IMetaClass getMetaClass(String name) {
		if (name != null && !name.isEmpty() && Character.isUpperCase(name.charAt(0))) {
			return new UnknownMetaClass(name, this);
		}
		return null;
	}

	@Override
	public List<IMetaClass> getTypes() {
		return Collections.emptyList();
	}

	@Override
	public List<Package> getSubPackages() {
		return Collections.emptyList();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UnknownMetamodel;
	}

	@Override
	public int hashCode() {
		return UnknownMetamodel.class.hashCode();
	}
}
