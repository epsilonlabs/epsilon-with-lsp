package org.eclipse.epsilon.eol.execute.operations.contributors;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.execute.operations.IMethodTypeCalculator;

public class ReturnTypeIsContainedType implements IMethodTypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, List<EolType> parameterTypes) {
		if (contextType instanceof EolCollectionType) {
			return ((EolCollectionType) contextType).getContentType();
		}
		return EolAnyType.Instance;
	}

}
