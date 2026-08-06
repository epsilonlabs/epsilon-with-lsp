/*********************************************************************
 * Copyright (c) 2026 The University of York.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.epsilon.eol.types;

import java.util.List;

import org.eclipse.epsilon.eol.exceptions.EolIllegalOperationParametersException;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;

public class EolTypeLiteral extends EolType {

	private final EolType wrappedType;

	public EolTypeLiteral(EolType wrappedType) {
		this.wrappedType = wrappedType;
	}

	@Override
	public String getName() {
		return "EolTypeLiteral<" + wrappedType.getName() + ">";
	}

	public EolType getWrappedType() {
		return wrappedType;
	}

	@Override
	public boolean isType(Object value) {
		return value instanceof EolType;
	}

	@Override
	public boolean isKind(Object value) {
		return isType(value);
	}

	@Override
	public EolType createInstance() {
		return wrappedType;
	}

	@Override
	public Object createInstance(List<Object> parameters) throws EolRuntimeException {
		throw new EolIllegalOperationParametersException("createInstance");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EolTypeLiteral;
	}

	@Override
	public int hashCode() {
		return EolTypeLiteral.class.hashCode();
	}
}
