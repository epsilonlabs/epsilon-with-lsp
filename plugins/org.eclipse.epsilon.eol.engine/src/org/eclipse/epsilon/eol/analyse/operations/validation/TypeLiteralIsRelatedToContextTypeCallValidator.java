package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Warns when a type test (e.g. <code>isKindOf(T)</code>,
 * <code>isTypeOf(T)</code>) can never succeed, because the type of the target
 * is unrelated to T. The type is expected to be the first parameter.
 */
public class TypeLiteralIsRelatedToContextTypeCallValidator implements IOperationCallValidator {

	@Override
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();
		EolType testedType = ElementTypeChecks.getLiteralType(ElementTypeChecks.getFirstParameterType(parameterTypes));
		if (testedType != null && !ElementTypeChecks.canBeOfType(contextType, testedType)) {
			markers.add(new ModuleMarker(element, "Type " + testedType + " is unrelated to " + contextType
					+ ", so this check is always false", Severity.Warning));
		}
		return markers;
	}
}
