package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Checks that the entries of the map passed to an operation that stores them
 * in another map (e.g. <code>putAll(m)</code>) are assignable to the key and
 * value types of the latter. The map is expected to be the first parameter.
 */
public class ParameterMapEntryTypesAreKeyAndValueTypesCallValidator implements IOperationCallValidator {

	@Override
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();
		EolType keyType = ElementTypeChecks.getKeyType(contextType);
		EolType valueType = ElementTypeChecks.getValueType(contextType);
		EolType parameterType = ElementTypeChecks.getFirstParameterType(parameterTypes);
		if (keyType != null && valueType != null && parameterType != null) {
			ElementTypeChecks.checkEntriesAssignable(markers, element, parameterType, keyType, valueType);
		}
		return markers;
	}
}
