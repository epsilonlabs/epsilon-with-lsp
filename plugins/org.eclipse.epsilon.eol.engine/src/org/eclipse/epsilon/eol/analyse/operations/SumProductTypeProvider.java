package org.eclipse.epsilon.eol.analyse.operations;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolNativeType;
import org.eclipse.epsilon.eol.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Returns Integer if the target collection holds Integers, Real if it holds
 * Integers and Reals, and Number otherwise.
 * Used by <code>sum()</code> and <code>product()</code>.
 */
public class SumProductTypeProvider implements IOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, List<EolType> parameterTypes) {
		EolType elementType = ElementTypes.of(contextType);
		if (TypeRelations.isKnownSubtype(elementType, EolPrimitiveType.Integer)) {
			return EolPrimitiveType.Integer;
		}
		if (TypeRelations.isKnownSubtype(elementType, EolPrimitiveType.Real)) {
			return EolPrimitiveType.Real;
		}
		return EolNativeType.Number;
	}

}
