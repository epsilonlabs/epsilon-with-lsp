package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Warns when an operation that selects the elements of a type (e.g.
 * <code>selectByKind(T)</code>, <code>selectByType(T)</code>) can never select
 * anything, because the contained type is unrelated to T. The type is expected
 * to be the first parameter.
 */
public class TypeLiteralIsRelatedToContainedTypeCallValidator implements IOperationCallValidator {

	@Override
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();
		EolType containedType = ElementTypeChecks.getContentType(contextType);
		EolType testedType = ElementTypeChecks.getLiteralType(ElementTypeChecks.getFirstParameterType(parameterTypes));
		if (containedType != null && testedType != null && !ElementTypeChecks.canBeOfType(containedType, testedType)) {
			markers.add(new ModuleMarker(element, "Type " + testedType + " is unrelated to contained type "
					+ containedType + ", so no element can match", Severity.Warning));
		}
		return markers;
	}
}
