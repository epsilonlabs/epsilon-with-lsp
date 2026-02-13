package org.eclipse.epsilon.eol.execute.operations.contributors;

import java.util.List;

import org.eclipse.epsilon.eol.execute.operations.IMethodTypeCalculator;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public class ReturnTypeIsContextType implements IMethodTypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, List<EolType> parameterTypes) {
		return contextType;
	}

}
