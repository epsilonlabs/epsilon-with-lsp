package org.eclipse.epsilon.eol.execute.operations;

import java.util.List;

import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public interface ITypeCalculator {
public EolType calculateType(EolType contextType, EolType iteratorType, List<EolType> expressionTypes);
}
