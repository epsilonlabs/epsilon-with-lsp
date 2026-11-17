/*******************************************************************************
 * Copyright (c) 2012 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Dimitrios Kolovos - initial API and implementation
 ******************************************************************************/
package org.eclipse.epsilon.eol.execute.operations.contributors;

import java.util.Collection;

import org.eclipse.epsilon.eol.analyse.operations.BagOfElementTypeProvider;
import org.eclipse.epsilon.eol.analyse.operations.OperationTypeProvider;
import org.eclipse.epsilon.eol.analyse.operations.OrderedSetOfElementTypeProvider;
import org.eclipse.epsilon.eol.analyse.operations.SequenceOfElementTypeProvider;
import org.eclipse.epsilon.eol.analyse.operations.SetOfElementTypeProvider;
import org.eclipse.epsilon.eol.types.EolBag;
import org.eclipse.epsilon.eol.types.EolOrderedSet;
import org.eclipse.epsilon.eol.types.EolSequence;
import org.eclipse.epsilon.eol.types.EolSet;

public class ScalarOperationContributor extends OperationContributor {
	
	@Override
	public boolean contributesTo(Object target) {
		return !(target instanceof Collection);
	}
	
	@OperationTypeProvider(SequenceOfElementTypeProvider.class)
	public EolSequence<Object> asSequence() {
		EolSequence<Object> sequence = new EolSequence<>();
		sequence.add(getTarget());
		return sequence;
	}
	
	@OperationTypeProvider(SetOfElementTypeProvider.class)
	public EolSet<Object> asSet() {
		EolSet<Object>set = new EolSet<>();
		set.add(getTarget());
		return set;
	}
	
	@OperationTypeProvider(BagOfElementTypeProvider.class)
	public EolBag<Object> asBag() {
		EolBag<Object> bag = new EolBag<>();
		bag.add(getTarget());
		return bag;
	}
	
	@OperationTypeProvider(OrderedSetOfElementTypeProvider.class)
	public EolOrderedSet<Object> asOrderedSet() {
		EolOrderedSet<Object> set = new EolOrderedSet<>();
		set.add(getTarget());
		return set;
	}
	
	public int size() {
		return 1;
	}
	
}
