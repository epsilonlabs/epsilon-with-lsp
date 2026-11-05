package org.eclipse.epsilon.eol.analyse.operations;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolType;

public class ReturnTypeIsContainedTypeProvider implements IOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, List<EolType> parameterTypes) {
		if (contextType instanceof EolCollectionType) {
			return ((EolCollectionType) contextType).getContentType();
		}
		return EolAnyType.Instance;
	}

}
