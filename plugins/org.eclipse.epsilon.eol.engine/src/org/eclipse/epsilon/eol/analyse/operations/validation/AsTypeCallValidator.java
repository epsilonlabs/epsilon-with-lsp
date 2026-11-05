package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.types.EolTypeLiteral;

/**
 * A diagnostics calculator that checks whether the parameter to asType()
 * is a compatible type literal rather than a regular value.
 */
public class AsTypeCallValidator implements IOperationCallValidator {

	@Override
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();

		if (parameterTypes == null || parameterTypes.isEmpty()) {
			return markers;
		}

		EolType parameterType = parameterTypes.get(0);

		if (!(parameterType instanceof EolTypeLiteral)) {
			markers.add(new ModuleMarker(element,
					"Expected type literal instead of " + parameterType.getName(),
					Severity.Error));
			return markers;
		}

		EolType castType = ((EolTypeLiteral) parameterType).getWrappedType();
		if (!contextType.isAssignableTo(castType) && !castType.isAssignableTo(contextType)) {
			markers.add(new ModuleMarker(element,
					contextType.getName() + " cannot be cast to " + castType.getName(),
					Severity.Error));
		}

		return markers;
	}
}
