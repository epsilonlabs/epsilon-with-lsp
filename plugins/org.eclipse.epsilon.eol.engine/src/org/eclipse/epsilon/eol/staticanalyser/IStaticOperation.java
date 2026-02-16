package org.eclipse.epsilon.eol.staticanalyser;

import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public interface IStaticOperation {

	public String getName();

	public EolType getContextType();

	public EolType getReturnType(EolType actualContextType, List<EolType> actualParameterTypes);

	public List<EolType> getParameterTypes();

	public List<ModuleMarker> getExtraDiagnostics(AbstractModuleElement element, EolType actualContextType,
			List<EolType> actualParameterTypes);
}
