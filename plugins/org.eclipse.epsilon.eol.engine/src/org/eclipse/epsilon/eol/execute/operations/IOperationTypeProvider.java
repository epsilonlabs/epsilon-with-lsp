package org.eclipse.epsilon.eol.execute.operations;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolType;

public interface IOperationTypeProvider {
	public EolType calculateType(EolType contextType, List<EolType> parameterTypes);
}
