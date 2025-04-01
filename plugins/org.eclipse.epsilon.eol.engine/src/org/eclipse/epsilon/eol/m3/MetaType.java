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

public abstract class MetaType extends NamedElement {
	protected IMetamodel metamodel;

	public IMetamodel getMetamodel() {
		return metamodel;
	}

	public void setMetamodel(IMetamodel metamodel) {
		this.metamodel = metamodel;
	}
}
