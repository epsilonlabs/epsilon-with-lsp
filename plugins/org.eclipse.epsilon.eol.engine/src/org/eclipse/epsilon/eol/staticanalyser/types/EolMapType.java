/*********************************************************************
* Copyright (c) 2008 The University of York.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.epsilon.eol.staticanalyser.types;

import org.eclipse.epsilon.eol.types.EolMap;

public class EolMapType extends EolType {
	
	public static final EolMapType
		Map = new EolMapType("Map"),
		ConcurrentMap = new EolMapType("ConcurrentMap");
	
	protected EolType keyType;
	protected EolType valueType;
	private final String name;
	
	/**
	 * 
	 * @param name
	 * @since 1.6
	 */
	public EolMapType(String name) {
		this(EolAnyType.Instance, EolAnyType.Instance, "ConcurrentMap".equals(name));
	}
	
	public EolMapType() {
		this("Map");
	}
	
	public EolMapType(EolType keyType, EolType valueType) {
		this(keyType, valueType, false);
	}
	
	public Class<?> getClazz(){
		return EolMap.class;
	}
	
	/**
	 * 
	 * @param keyType
	 * @param valueType
	 * @param concurrent
	 * @since 1.6
	 */
	public EolMapType(EolType keyType, EolType valueType, boolean concurrent) {
		this.keyType = keyType;
		this.valueType = valueType;
		this.name = concurrent ? "ConcurrentMap" : "Map";
	}

	/**
	 * 
	 * @return
	 * @since 1.6
	 */
	public boolean isConcurrent() {
		return name.contains("Concurrent");
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public void setKeyType(EolType keyType) {
		this.keyType = keyType;
	}
	
	public EolType getKeyType() {
		return keyType;
	}
	
	public void setValueType(EolType valueType) {
		this.valueType = valueType;
	}
	
	public EolType getValueType() {
		return valueType;
	}
	
	@Override
	public String toString() {
		return getName()+"<" + keyType + ", " + valueType + ">";
	}
}
