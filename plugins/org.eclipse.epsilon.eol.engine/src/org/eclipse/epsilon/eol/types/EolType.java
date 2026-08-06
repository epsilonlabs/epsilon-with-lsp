/*******************************************************************************
 * Copyright (c) 2008 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Dimitrios Kolovos - initial API and implementation
 ******************************************************************************/
package org.eclipse.epsilon.eol.types;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;

public abstract class EolType {

	public Class<?> getClazz() {
		return Object.class;
	}

	public abstract String getName();
	
	public abstract boolean isType(Object o);
	
	public abstract boolean isKind(Object o);
	
	public abstract Object createInstance() throws EolRuntimeException;
	
	public abstract Object createInstance(List<Object> parameters) throws EolRuntimeException;

	public boolean isNot(EolType type) {
		return !equals(type);
	}

	public boolean isAbstract() {
		return false;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(toString());
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null) return false;
		
		if (this.getClass() != other.getClass()) return false;
		
		EolType eolType = (EolType) other;
		
		return Objects.equals(this.toString(), eolType.toString());
	}
	
	public List<EolType> getParentTypes() {
		EolType parentType = getParentType();
		if (parentType == null) return Collections.emptyList();
		else return Arrays.asList(parentType);
	}
	
	protected EolType getParentType() {
		return new EolNativeType(Object.class);
	}

	public List<EolType> getChildrenTypes(){
		return Collections.emptyList();
	}

	public Set<EolType> getAncestors() {
		Set<EolType> ancestors = new LinkedHashSet<>();
		Deque<EolType> remaining = new ArrayDeque<>();
		remaining.push(this);

		while (!remaining.isEmpty()) {
			EolType type = remaining.pop();
			if (ancestors.add(type)) {
				for (EolType parentType : type.getParentTypes()) {
					remaining.push(parentType);
				}
			}
		}
		return ancestors;
	}

	public boolean isAncestorOf(EolType type) {
		return !EolNoType.Instance.equals(type) && type.getAncestors().contains(this);
	}

	public boolean isSiblingOf(EolType type) {
		if (EolNoType.Instance.equals(type) || EolAnyType.Instance.equals(type)) {
			return false;
		}
		return !Collections.disjoint(getParentTypes(), type.getParentTypes());
	}

	public boolean isAssignableTo(EolType targetType) {
		return equals(targetType)
			|| EolAnyType.Instance.equals(this)
			|| EolAnyType.Instance.equals(targetType)
			|| targetType.isAncestorOf(this);
	}
}
