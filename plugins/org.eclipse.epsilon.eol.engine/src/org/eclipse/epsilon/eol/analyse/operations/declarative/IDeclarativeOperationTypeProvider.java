package org.eclipse.epsilon.eol.analyse.operations.declarative;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolType;

public interface IDeclarativeOperationTypeProvider {
	public EolType getType(EolType contextType, EolType iteratorType, List<EolType> expressionTypes);
}
