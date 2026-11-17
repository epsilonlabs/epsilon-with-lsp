package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Checks that the elements of the collection passed to an operation that looks
 * them up in another collection (e.g. <code>removeAll(c)</code>,
 * <code>includesAll(c)</code>, <code>excludingAll(c)</code>) could be equal to
 * elements of the latter. The collection is expected to be the first parameter.
 */
public class ParameterContentTypeIsComparableToContainedTypeCallValidator implements IOperationCallValidator {

	@Override
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();
		EolType containedType = ElementTypeChecks.getContentType(contextType);
		EolType parameterType = ElementTypeChecks.getFirstParameterType(parameterTypes);
		if (containedType != null && parameterType != null) {
			ElementTypeChecks.checkContentComparable(markers, element, parameterType, containedType);
		}
		return markers;
	}
}
