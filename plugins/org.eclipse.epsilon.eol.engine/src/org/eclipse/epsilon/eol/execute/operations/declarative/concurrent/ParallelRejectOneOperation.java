/*********************************************************************
 * Copyright (c) 2018 The University of York.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.epsilon.eol.execute.operations.declarative.concurrent;

import org.eclipse.epsilon.eol.execute.operations.TypeCalculator;
import org.eclipse.epsilon.eol.execute.operations.declarative.RejectOneOperation;
import org.eclipse.epsilon.eol.execute.operations.declarative.SelectTypeCalculator;

/**
 * 
 * @author Sina Madani
 * @since 1.6
 */
@TypeCalculator(klass = SelectTypeCalculator.class)
public class ParallelRejectOneOperation extends RejectOneOperation {

	public ParallelRejectOneOperation() {
		setDelegateOperation(new ParallelSelectOperation());
	}

}
