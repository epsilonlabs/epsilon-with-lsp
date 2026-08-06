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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;

public class EolUnionType extends EolType {

	public final Set<EolType> containedTypes = new LinkedHashSet<>();

	public EolUnionType(EolType... types) {
		for (EolType type : types) {
			containedTypes.add(type);
		}
	}

	public EolUnionType(Collection<? extends EolType> types) {
		containedTypes.addAll(types);
	}

	@Override
	public String getName() {
		return containedTypes.stream().map(EolType::getName).collect(Collectors.joining("|"));
	}

	@Override
	public String toString() {
		return containedTypes.stream().map(EolType::toString).collect(Collectors.joining("|"));
	}

	@Override
	public boolean isType(Object value) {
		return containedTypes.stream().anyMatch(type -> type.isType(value));
	}

	@Override
	public boolean isKind(Object value) {
		return containedTypes.stream().anyMatch(type -> type.isKind(value));
	}

	@Override
	public Object createInstance() throws EolRuntimeException {
		throw new EolRuntimeException("Union types cannot be instantiated");
	}

	@Override
	public Object createInstance(List<Object> parameters) throws EolRuntimeException {
		return createInstance();
	}

	@Override
	public boolean isAbstract() {
		return true;
	}

	@Override
	public boolean isAncestorOf(EolType type) {
		return containedTypes.stream().anyMatch(containedType -> containedType.isAncestorOf(type));
	}

	@Override
	public List<EolType> getParentTypes() {
		Set<EolType> parents = containedTypes.stream()
			.flatMap(type -> type.getParentTypes().stream())
			.collect(Collectors.toSet());
		List<EolType> result = new ArrayList<>();
		result.add(parents.size() == 1 ? parents.iterator().next() : new EolUnionType(parents));
		return result;
	}

	@Override
	public List<EolType> getChildrenTypes() {
		return new ArrayList<>(containedTypes);
	}
}
