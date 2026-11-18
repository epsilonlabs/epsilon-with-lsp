package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Checks that the value passed to <code>containsValue(v)</code> could be equal
 * to a value of the target map.
 */
public class ContainsValueCallValidator implements IOperationCallValidator {

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
