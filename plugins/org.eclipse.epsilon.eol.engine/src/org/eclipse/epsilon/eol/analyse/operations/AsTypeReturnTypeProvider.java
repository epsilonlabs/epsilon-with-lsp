package org.eclipse.epsilon.eol.analyse.operations;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.types.EolTypeLiteral;

public class AsTypeReturnTypeProvider implements IOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, List<EolType> parameterTypes) {
		if(parameterTypes.get(0) instanceof EolTypeLiteral) {
			return ((EolTypeLiteral) parameterTypes.get(0)).getWrappedType();
		}
		return EolAnyType.Instance;
	}

}
