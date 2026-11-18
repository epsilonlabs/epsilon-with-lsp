package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Checks the arguments of <code>getOrDefault(k, default)</code>: the key could
 * be equal to a key of the map, and the default is assignable to the value
 * type, since it is returned in place of a value.
 */
public class GetOrDefaultCallValidator implements IOperationCallValidator {

	@Override
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();
		EolType keyType = ElementTypeChecks.getKeyType(contextType);
		EolType valueType = ElementTypeChecks.getValueType(contextType);
		EolType keyParameterType = ElementTypeChecks.getFirstParameterType(parameterTypes);
		EolType defaultParameterType = ElementTypeChecks.getLastParameterType(parameterTypes);
		if (keyType != null && keyParameterType != null) {
			ElementTypeChecks.checkComparable(markers, element, keyParameterType, keyType, "key");
		}
		if (valueType != null && defaultParameterType != null) {
			ElementTypeChecks.checkAssignable(markers, element, defaultParameterType, valueType);
		}
		return markers;
	}
}
