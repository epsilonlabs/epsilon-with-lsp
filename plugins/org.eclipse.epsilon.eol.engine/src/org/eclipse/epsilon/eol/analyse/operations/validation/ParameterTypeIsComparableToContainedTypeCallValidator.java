package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Checks that the element passed to an operation that looks it up in a
 * collection (e.g. <code>remove(e)</code>, <code>includes(e)</code>,
 * <code>indexOf(e)</code>) could be equal to an element of the collection. The
 * element is expected to be the first parameter.
 */
public class ParameterTypeIsComparableToContainedTypeCallValidator implements IOperationCallValidator {

	@Override
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();
		EolType containedType = ElementTypeChecks.getContentType(contextType);
		EolType parameterType = ElementTypeChecks.getFirstParameterType(parameterTypes);
		if (containedType != null && parameterType != null) {
			ElementTypeChecks.checkComparable(markers, element, parameterType, containedType, "contained");
		}
		return markers;
	}
}
