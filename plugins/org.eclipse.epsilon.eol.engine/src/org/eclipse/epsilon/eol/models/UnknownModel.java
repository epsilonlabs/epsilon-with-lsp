/*********************************************************************
* Copyright (c) 2008 The University of York.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.epsilon.eol.models;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.m3.IMetamodel;
import org.eclipse.epsilon.eol.m3.UnknownMetamodel;

/**
 * A model for the Unknown EMC driver. This driver treats all
 * capitalised type names (e.g. Task, Person) as valid model element
 * types, enabling partial static analysis when the actual metamodel
 * is not available.
 */
public class UnknownModel extends EmptyModel {

	@Override
	public IMetamodel getMetamodel(StringProperties properties, IRelativePathResolver resolver) {
		return new UnknownMetamodel();
	}
}
