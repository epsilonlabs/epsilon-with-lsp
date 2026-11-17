package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Checks that the value passed to an operation that looks it up in a map (e.g.
 * <code>containsValue(v)</code>) could be equal to a value of the map. The
 * value is expected to be the first parameter.
 */
public class ParameterTypeIsComparableToValueTypeCallValidator implements IOperationCallValidator {

	@Override
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();
		EolType valueType = ElementTypeChecks.getValueType(contextType);
		EolType parameterType = ElementTypeChecks.getFirstParameterType(parameterTypes);
		if (valueType != null && parameterType != null) {
			ElementTypeChecks.checkComparable(markers, element, parameterType, valueType, "value");
		}
		return markers;
	}
}
