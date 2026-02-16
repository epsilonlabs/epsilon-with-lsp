package org.eclipse.epsilon.eol.execute.operations;

import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public interface IMethodDiagnosticsCalculator {
	public List<ModuleMarker> calculateDiagnostics(AbstractModuleElement element, EolType contextType, List<EolType> parameterTypes);
}
