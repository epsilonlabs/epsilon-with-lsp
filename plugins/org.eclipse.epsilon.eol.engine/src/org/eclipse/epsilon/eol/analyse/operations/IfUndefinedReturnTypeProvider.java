package org.eclipse.epsilon.eol.analyse.operations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolNoType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.types.EolUnionType;

/**
 * Returns the type of <code>ifUndefined(alternative)</code>, which is either
 * the target or the alternative.
 */
public class IfUndefinedReturnTypeProvider implements IOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, List<EolType> parameterTypes) {
		EolType alternativeType = parameterTypes == null || parameterTypes.isEmpty() ? null : parameterTypes.get(0);
		if (isUnknown(contextType)) {
			return isUnknown(alternativeType) ? EolAnyType.Instance : alternativeType;
		}
		if (isUnknown(alternativeType)) {
			return contextType;
		}
		if (contextType instanceof EolAnyType || alternativeType instanceof EolAnyType) {
			return EolAnyType.Instance;
		}
		if (alternativeType.isAssignableTo(contextType)) {
			return contextType;
		}
		if (contextType.isAssignableTo(alternativeType)) {
			return alternativeType;
		}
		Set<EolType> types = new LinkedHashSet<>();
		addFlattened(contextType, types);
		addFlattened(alternativeType, types);
		return new EolUnionType(types);
	}

	protected boolean isUnknown(EolType type) {
		return type == null || EolNoType.Instance.equals(type);
	}

	protected void addFlattened(EolType type, Set<EolType> types) {
		if (type instanceof EolUnionType) {
			types.addAll(((EolUnionType) type).containedTypes);
		}
		else {
			types.add(type);
		}
	}

}
