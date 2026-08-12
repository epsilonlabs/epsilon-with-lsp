/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.epsilon.eol.types;

import java.util.List;
import java.util.Objects;

import org.eclipse.epsilon.eol.exceptions.EolIllegalOperationParametersException;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.m3.IDataType;

public class EolDataType extends EolType {

	protected final IDataType dataType;
	protected final String name;

	public EolDataType(IDataType dataType) {
		this(dataType, dataType.getName());
	}

	public EolDataType(IDataType dataType, String name) {
		this.dataType = Objects.requireNonNull(dataType, "dataType");
		this.name = name;
	}

	public IDataType getDataType() {
		return dataType;
	}

	@Override
	public Class<?> getClazz() {
		return dataType.getClazz();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isType(Object value) {
		return getClazz() != null && getClazz().isInstance(value);
	}

	@Override
	public boolean isKind(Object value) {
		return isType(value);
	}

	@Override
	public Object createInstance() throws EolRuntimeException {
		throw new EolIllegalOperationParametersException("createInstance");
	}

	@Override
	public Object createInstance(List<Object> parameters) throws EolRuntimeException {
		throw new EolIllegalOperationParametersException("createInstance");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EolDataType
			&& Objects.equals(dataType, ((EolDataType) other).dataType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dataType);
	}
}
