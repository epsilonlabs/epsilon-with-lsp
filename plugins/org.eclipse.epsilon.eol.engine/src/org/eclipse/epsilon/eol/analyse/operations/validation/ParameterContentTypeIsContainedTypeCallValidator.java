package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Checks that the elements of the collection passed to an operation that
 * stores them in another collection (e.g. <code>addAll(c)</code>,
 * <code>includingAll(c)</code>) are assignable to the contained type of the
 * latter. The collection is expected to be the last parameter.
 */
public class ParameterContentTypeIsContainedTypeCallValidator implements IOperationCallValidator {

	@Override
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();
		EolType containedType = ElementTypeChecks.getContentType(contextType);
		EolType parameterType = ElementTypeChecks.getLastParameterType(parameterTypes);
		if (containedType != null && parameterType != null) {
			ElementTypeChecks.checkContentAssignable(markers, element, parameterType, containedType);
		}
		return markers;
	}
}
