package org.eclipse.epsilon.eol.analyse.operations;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolType;

public interface IOperationTypeProvider {
	public EolType getType(EolType contextType, List<EolType> parameterTypes);
}
