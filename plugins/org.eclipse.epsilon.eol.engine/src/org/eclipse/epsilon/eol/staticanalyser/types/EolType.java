/*******************************************************************************
 * Copyright (c) 2008 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Dimitrios Kolovos - initial API and implementation
 ******************************************************************************/
package org.eclipse.epsilon.eol.staticanalyser.types;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

public abstract class EolType {
	private Set<EolType> ancestorCache = null; 

	public Class<?> getClazz(){
		return java.lang.Object.class;
	}

	public abstract String getName();

	public boolean isNot(EolType type) {
		return !this.equals(type);
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
		if (this == other)
			return true;
		if (other == null)
			return false;

		if (this.getClass() != other.getClass())
			return false;

		EolType eolType = (EolType) other;

		return Objects.equals(this.toString(), eolType.toString());
	}

	public List<EolType> getParentTypes() {
		EolType parentType = getParentType();
		if (parentType == null)
			return Collections.emptyList();
		else
			return Arrays.asList(parentType);
	}

	protected EolType getParentType() {
		return new EolNativeType(java.lang.Object.class);
	}

	public Set<EolType> getAncestors() {
		if (ancestorCache != null) {
			return ancestorCache;
		}
		Set<EolType> ancestors = new LinkedHashSet<EolType>();
		
		Stack<EolType> stack = new Stack<EolType>();
		stack.push(this);
		while (!stack.isEmpty()) {
			EolType currentNode = stack.pop();
			if (!ancestors.contains(currentNode)) {
				ancestors.add(currentNode);
				stack.addAll(currentNode.getParentTypes());
			}
		}
		
		ancestorCache = ancestors;
		return ancestors;
	}

	public List<EolType> getChildrenTypes() {
		return Collections.emptyList();
	}

	public boolean isAncestorOf(EolType type) {
		if (type.equals(EolNoType.Instance)) {
			return false;
		}
		return type.getAncestors().contains(this);
	}
	
	public boolean isAssignableTo(EolType targetType) {
		if (targetType.equals(this)) {
			return true;
		}
		if (this instanceof EolAnyType || targetType instanceof EolAnyType) {
			return true;
		}
		if (targetType.isAncestorOf(this)) {
			return true;
		}
		else {
			return false;
		}
	}
}
