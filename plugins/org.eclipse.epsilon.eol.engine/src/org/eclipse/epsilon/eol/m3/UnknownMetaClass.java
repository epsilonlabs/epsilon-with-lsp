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

import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * A meta class for the Unknown EMC driver. Accepts any property access
 * without errors since the actual metamodel is not known at static
 * analysis time.
 */
public class UnknownMetaClass extends MetaClass {

	private String name;

	public UnknownMetaClass(String name, IMetamodel metamodel) {
		this.name = name;
		this.metamodel = metamodel;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<?> getClazz() {
		return null;
	}

	/**
	 * Returns a property with type Any for any name, since the actual
	 * metamodel is not known.
	 */
	@Override
	public IProperty getProperty(String name) {
		return new IProperty() {
			@Override
			public EolType getType() {
				return EolAnyType.Instance;
			}

			@Override
			public String getName() {
				return name;
			}
		};
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof UnknownMetaClass) {
			return name.equals(((UnknownMetaClass) other).name);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
