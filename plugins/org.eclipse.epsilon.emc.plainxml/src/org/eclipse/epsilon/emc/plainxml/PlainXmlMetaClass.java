/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *******************************************************************************/
package org.eclipse.epsilon.emc.plainxml;

import java.util.Objects;

import org.eclipse.epsilon.eol.m3.MetaClass;
import org.w3c.dom.Element;

public class PlainXmlMetaClass extends MetaClass {

	public PlainXmlMetaClass(String name, PlainXmlModelMetamodel metamodel) {
		this.name = name;
		this.metamodel = metamodel;
	}

	@Override
	public Class<?> getClazz() {
		return Element.class;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof PlainXmlMetaClass)) return false;
		PlainXmlMetaClass otherMetaClass = (PlainXmlMetaClass) other;
		return Objects.equals(name, otherMetaClass.name)
			&& Objects.equals(metamodel, otherMetaClass.metamodel);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, metamodel);
	}
}
