package org.eclipse.epsilon.eol.execute.operations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.eol.staticanalyser.types.EolCollectionType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

/**
 * A diagnostics calculator that checks whether the parameter type is compatible
 * with the contained type of a collection.
 */
public class ParameterTypeIsContainedType implements IMethodDiagnosticsCalculator {

	@Override
	public List<ModuleMarker> calculateDiagnostics(AbstractModuleElement element, EolType contextType,
			List<EolType> parameterTypes) {
		List<ModuleMarker> markers = new ArrayList<>();

		if (!(contextType instanceof EolCollectionType)) {
			return markers;
		}

		if (parameterTypes == null || parameterTypes.isEmpty()) {
			return markers;
		}

		EolCollectionType collectionType = (EolCollectionType) contextType;
		EolType containedType = collectionType.getContentType();
		EolType parameterType = parameterTypes.get(0);

		if (!parameterType.isAssignableTo(containedType)) {
			ModuleMarker marker = new ModuleMarker(
					element, "Parameter Type " + parameterType.getName()
							+ " cannot be assigned to expected parameter type " + containedType.getName(),
					Severity.Error);
			markers.add(marker);
		}

		return markers;
	}
}
