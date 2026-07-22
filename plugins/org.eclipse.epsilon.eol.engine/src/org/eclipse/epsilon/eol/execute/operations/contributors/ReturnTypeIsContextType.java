package org.eclipse.epsilon.eol.execute.operations.contributors;

import java.util.List;

import org.eclipse.epsilon.eol.analyse.types.EolType;
import org.eclipse.epsilon.eol.execute.operations.IMethodTypeCalculator;

public class ReturnTypeIsContextType implements IMethodTypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, List<EolType> parameterTypes) {
		return contextType;
	}

}
