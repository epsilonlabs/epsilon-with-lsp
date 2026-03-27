/*********************************************************************
 * Copyright (c) 2020 The University of York.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.epsilon.eol.staticanalyser.types;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Sina Madani
 * @since 2.2
 */
public class EolTupleType extends EolType {

	protected Map<String, EolType> propertyTypes = new HashMap<>();

	@Override
	public String getName() {
		return "Tuple";
	}

	public void setPropertyType(String name, EolType type) {
		propertyTypes.put(name, type);
	}

	public EolType getPropertyType(String name) {
		return propertyTypes.get(name);
	}

	public boolean hasProperty(String name) {
		return propertyTypes.containsKey(name);
	}
}
