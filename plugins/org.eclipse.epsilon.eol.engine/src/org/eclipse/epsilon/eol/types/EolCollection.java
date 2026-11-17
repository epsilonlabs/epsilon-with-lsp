/*********************************************************************
 * Copyright (c) 2020 The University of York.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.epsilon.eol.types;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.epsilon.eol.analyse.operations.validation.OperationCallValidator;
import org.eclipse.epsilon.eol.analyse.operations.validation.ParameterTypeIsContainedTypeCallValidator;
import org.eclipse.epsilon.eol.analyse.operations.validation.ParameterContentTypeIsComparableToContainedTypeCallValidator;
import org.eclipse.epsilon.eol.analyse.operations.validation.ParameterContentTypeIsContainedTypeCallValidator;
import org.eclipse.epsilon.eol.analyse.operations.validation.ParameterTypeIsComparableToContainedTypeCallValidator;

/**
 * Base class for all EOL collections.
 * 
 * @author Sina Madani
 * @since 1.6
 * @param <T> Type of elements.
 */
public abstract class EolCollection<T> implements Collection<T> {

	protected Collection<T> wrapped;
	
	protected EolCollection(Collection<T> delegate) {
		this.wrapped = delegate;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Collection)) return false;
		return obj instanceof EolCollection ?
			this.getClass() == obj.getClass() && this.wrapped.equals(((EolCollection<?>) obj).wrapped) :
			this.wrapped.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(wrapped, getClass().getName());
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+" "+wrapped.toString();
	}
	
	@Override
	@OperationCallValidator(ParameterTypeIsContainedTypeCallValidator.class)
	public boolean add(T e) {
		return wrapped.add(e);
	}

	@Override
	@OperationCallValidator(ParameterContentTypeIsContainedTypeCallValidator.class)
	public boolean addAll(Collection<? extends T> c) {
		return wrapped.addAll(c);
	}

	@Override
	public void clear() {
		wrapped.clear();
	}

	@Override
	@OperationCallValidator(ParameterTypeIsComparableToContainedTypeCallValidator.class)
	public boolean contains(Object o) {
		return wrapped.contains(o);
	}

	@Override
	@OperationCallValidator(ParameterContentTypeIsComparableToContainedTypeCallValidator.class)
	public boolean containsAll(Collection<?> c) {
		return wrapped.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return wrapped.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return wrapped.iterator();
	}

	@Override
	@OperationCallValidator(ParameterTypeIsComparableToContainedTypeCallValidator.class)
	public boolean remove(Object o) {
		return wrapped.remove(o);
	}

	@Override
	@OperationCallValidator(ParameterContentTypeIsComparableToContainedTypeCallValidator.class)
	public boolean removeAll(Collection<?> c) {
		return wrapped.removeAll(c);
	}

	@Override
	@OperationCallValidator(ParameterContentTypeIsComparableToContainedTypeCallValidator.class)
	public boolean retainAll(Collection<?> c) {
		return wrapped.retainAll(c);
	}

	@Override
	public int size() {
		return wrapped.size();
	}

	@Override
	public Object[] toArray() {
		return wrapped.toArray();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object[] toArray(Object[] a) {
		return wrapped.toArray(a);
	}
	
	@Override
	public void forEach(Consumer<? super T> action) {
		wrapped.forEach(action);
	}
	
	@Override
	public boolean removeIf(Predicate<? super T> filter) {
		return wrapped.removeIf(filter);
	}
	
	@Override
	public Stream<T> parallelStream() {
		return wrapped.parallelStream();
	}
	
	@Override
	public Stream<T> stream() {
		return wrapped.stream();
	}
	
	@Override
	public Spliterator<T> spliterator() {
		return wrapped.spliterator();
	}
}
