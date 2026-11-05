package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolType;

public interface IOperationCallValidator {
	public List<ModuleMarker> validate(AbstractModuleElement element, EolType contextType, List<EolType> parameterTypes);
}
