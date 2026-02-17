package org.eclipse.epsilon.eol.execute.operations.contributors;

import java.util.List;

import org.eclipse.epsilon.eol.execute.operations.IMethodTypeCalculator;
import org.eclipse.epsilon.eol.staticanalyser.types.EolAnyType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolCollectionType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public class ReturnTypeIsContainedType implements IMethodTypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, List<EolType> parameterTypes) {
		if (contextType instanceof EolCollectionType) {
			return ((EolCollectionType) contextType).getContentType();
		}
		return EolAnyType.Instance;
	}

}
