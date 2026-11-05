package org.eclipse.epsilon.eol.execute.operations.contributors.typeproviders;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.execute.operations.IOperationTypeProvider;

public class ReturnTypeIsContextTypeProvider implements IOperationTypeProvider {

	@Override
	public EolType calculateType(EolType contextType, List<EolType> parameterTypes) {
		return contextType;
	}

}
