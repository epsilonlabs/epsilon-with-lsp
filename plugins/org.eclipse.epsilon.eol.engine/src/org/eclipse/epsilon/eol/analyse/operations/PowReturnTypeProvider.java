package org.eclipse.epsilon.eol.analyse.operations;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.types.EolUnionType;

/**
 * Returns Integer when an Integer is raised to an Integer power, and Real when
 * either number is a Real.
 * Used by <code>pow(n)</code>.
 */
public class PowReturnTypeProvider implements IOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, List<EolType> parameterTypes) {
		EolType exponentType = parameterTypes.isEmpty() ? EolAnyType.Instance : parameterTypes.get(0);
		if (TypeRelations.isKnownSubtype(contextType, EolPrimitiveType.Integer)) {
			if (TypeRelations.isKnownSubtype(exponentType, EolPrimitiveType.Integer)) {
				return EolPrimitiveType.Integer;
			}
			if (TypeRelations.isKnownSubtype(exponentType, EolPrimitiveType.Real)
					&& !TypeRelations.maybeSubtype(exponentType, EolPrimitiveType.Integer)) {
				return EolPrimitiveType.Real;
			}
			return new EolUnionType(EolPrimitiveType.Integer, EolPrimitiveType.Real);
		}
		if (TypeRelations.isKnownSubtype(contextType, EolPrimitiveType.Real)) {
			return EolPrimitiveType.Real;
		}
		return contextType;
	}

}
