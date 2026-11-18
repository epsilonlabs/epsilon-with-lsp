package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Checks that the key and value passed to a map's <code>remove(k, v)</code>
 * could be equal to a key and a value of the map. The single-parameter
 * <code>remove</code> of maps and collections use the generic key and element
 * checks instead.
 */
public class RemoveCallValidator implements IOperationCallValidator {

	@Override
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();
		EolType keyType = ElementTypeChecks.getKeyType(contextType);
		EolType valueType = ElementTypeChecks.getValueType(contextType);
		EolType keyParameterType = ElementTypeChecks.getFirstParameterType(parameterTypes);
		EolType valueParameterType = ElementTypeChecks.getLastParameterType(parameterTypes);
		if (keyType != null && keyParameterType != null) {
			ElementTypeChecks.checkComparable(markers, element, keyParameterType, keyType, "key");
		}
		if (valueType != null && valueParameterType != null) {
			ElementTypeChecks.checkComparable(markers, element, valueParameterType, valueType, "value");
		}
		return markers;
	}
}
