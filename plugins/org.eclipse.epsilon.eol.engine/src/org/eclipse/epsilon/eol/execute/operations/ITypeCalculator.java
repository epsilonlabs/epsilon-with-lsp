package org.eclipse.epsilon.eol.execute.operations;

import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public interface ITypeCalculator {
public EolType calculateType(EolType contextType, EolType expressionType);
}
