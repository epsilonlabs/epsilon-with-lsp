package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Checks that the element passed to an operation that stores it in a
 * collection (e.g. <code>add(e)</code>, <code>add(index, e)</code>,
 * <code>including(e)</code>) is assignable to the contained type of the
 * collection. The element is expected to be the last parameter.
 */
public class ParameterTypeIsContainedTypeCallValidator implements IOperationCallValidator {

	@Override
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();
		EolType containedType = ElementTypeChecks.getContentType(contextType);
		EolType parameterType = ElementTypeChecks.getLastParameterType(parameterTypes);
		if (containedType != null && parameterType != null) {
			ElementTypeChecks.checkAssignable(markers, element, parameterType, containedType);
		}
		return markers;
	}
}
