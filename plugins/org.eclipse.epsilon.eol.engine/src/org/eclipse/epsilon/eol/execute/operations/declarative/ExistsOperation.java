/*******************************************************************************
 * Copyright (c) 2008-2018 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Dimitrios Kolovos - initial API and implementation
 *     Sina Madani - refactoring
 ******************************************************************************/
package org.eclipse.epsilon.eol.execute.operations.declarative;

import java.util.List;
import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.dom.NameExpression;
import org.eclipse.epsilon.eol.dom.Parameter;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.operations.TypeCalculator;

@TypeCalculator(klass = BooleanTypeCalculator.class)
public class ExistsOperation extends SelectBasedOperation {

	@Override
	public Boolean execute(Object target, NameExpression operationNameExpression, List<Parameter> iterators, List<Expression> expressions, IEolContext context) throws EolRuntimeException {
		return !getDelegateOperation().execute(true, target, operationNameExpression, iterators, expressions.get(0), context).isEmpty();
	}
	
}
