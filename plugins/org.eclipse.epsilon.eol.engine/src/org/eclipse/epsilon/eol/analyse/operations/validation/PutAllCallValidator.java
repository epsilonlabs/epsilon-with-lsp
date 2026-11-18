package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Checks that the entries of the map passed to <code>putAll(m)</code> are
 * assignable to the key and value types of the target map.
 */
public class PutAllCallValidator implements IOperationCallValidator {

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
