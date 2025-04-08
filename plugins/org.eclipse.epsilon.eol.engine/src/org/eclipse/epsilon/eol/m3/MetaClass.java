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

public abstract class MetaClass implements IMetaClass{
	
	protected List<IMetaClass> superTypes = new ArrayList<>();
	protected List<IMetaClass> subTypes = new ArrayList<>();
	protected List<Property> properties = new ArrayList<>();
	protected boolean isAbstract;
	protected IMetamodel metamodel;
	protected String name;

	public String getName() {
		return name;
	}
	
	public List<IMetaClass> getSuperTypes() {
		return superTypes;
	}
	
	public List<IMetaClass> getSubTypes() {
		return subTypes;
	}
	
	public List<Property> getProperties() {
		return properties;
	}
	
	public List<Property> getAllProperties() {
		List<Property> allProperties = new ArrayList<>();
		for (IMetaClass superType : superTypes) {
			allProperties.addAll(superType.getAllProperties());
		}
		allProperties.addAll(getProperties());
		return allProperties;
	}
	
	public boolean isAbstract() {
		return isAbstract;
	}
	
	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}
	
	public Property getProperty(String name) {
		for (Property property : getAllProperties()) {
			if (property.getName().equals(name)) {
				return property;
			}
		}
		return null;
	}
	
	public abstract boolean equals(Object other);
	
	public abstract int hashCode();

	public IMetamodel getMetamodel() {
		return metamodel;
	}
}
