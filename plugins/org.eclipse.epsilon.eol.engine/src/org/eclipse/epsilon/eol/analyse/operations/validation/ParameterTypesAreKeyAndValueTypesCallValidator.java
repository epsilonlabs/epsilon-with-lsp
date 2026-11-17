package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Checks that the key and values passed to an operation that stores them in a
 * map (e.g. <code>put(k, v)</code>, <code>replace(k, oldValue, newValue)</code>)
 * are assignable to the key and value types of the map. The key is expected
 * to be the first parameter and all remaining parameters are expected to be
 * values.
 */
public class ParameterTypesAreKeyAndValueTypesCallValidator implements IOperationCallValidator {

	@Override
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();
		EolType keyType = ElementTypeChecks.getKeyType(contextType);
		EolType valueType = ElementTypeChecks.getValueType(contextType);
		if (keyType == null || valueType == null || parameterTypes == null) {
			return markers;
		}
		for (int i = 0; i < parameterTypes.size(); i++) {
			ElementTypeChecks.checkAssignable(markers, element, parameterTypes.get(i), i == 0 ? keyType : valueType);
		}
		return markers;
	}
}
