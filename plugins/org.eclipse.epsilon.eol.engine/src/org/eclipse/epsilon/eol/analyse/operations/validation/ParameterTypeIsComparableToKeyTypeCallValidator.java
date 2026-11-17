package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Checks that the key passed to an operation that looks it up in a map (e.g.
 * <code>get(k)</code>, <code>containsKey(k)</code>, <code>remove(k)</code>)
 * could be equal to a key of the map. The key is expected to be the first
 * parameter.
 */
public class ParameterTypeIsComparableToKeyTypeCallValidator implements IOperationCallValidator {

	@Override
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();
		EolType keyType = ElementTypeChecks.getKeyType(contextType);
		EolType parameterType = ElementTypeChecks.getFirstParameterType(parameterTypes);
		if (keyType != null && parameterType != null) {
			ElementTypeChecks.checkComparable(markers, element, parameterType, keyType, "key");
		}
		return markers;
	}
}
