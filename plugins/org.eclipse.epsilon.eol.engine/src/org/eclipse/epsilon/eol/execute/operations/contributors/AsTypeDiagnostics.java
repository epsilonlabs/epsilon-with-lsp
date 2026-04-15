package org.eclipse.epsilon.eol.execute.operations.contributors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.eol.execute.operations.IMethodDiagnosticsCalculator;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolTypeLiteral;

/**
 * A diagnostics calculator that checks whether the parameter to asType()
 * is a type literal rather than a regular value.
 */
public class AsTypeDiagnostics implements IMethodDiagnosticsCalculator {

	@Override
	public List<ModuleMarker> calculateDiagnostics(AbstractModuleElement element, EolType contextType,
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
		}

		return markers;
	}
}
