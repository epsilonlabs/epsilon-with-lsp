package org.eclipse.epsilon.eol.execute.operations;

import java.util.List;

import org.eclipse.epsilon.eol.analyse.types.EolType;

public interface IMethodTypeCalculator {
	public EolType calculateType(EolType contextType, List<EolType> parameterTypes);
}
