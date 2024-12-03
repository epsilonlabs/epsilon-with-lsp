/*******************************************************************************
 * Copyright (c) 2008-2020 The University of York, Antonio García-Domínguez.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Dimitrios Kolovos - initial API and implementation
 *     Antonio García Domínguez - add generics, clean up dead code,
 *                                remove type cache (bug #410403).
 *    Sina Madani - concurrent types
 ******************************************************************************/
package org.eclipse.epsilon.eol.staticanalyser.types;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EolCollectionType extends EolType {
	
	protected EolType contentType = EolAnyType.Instance;
	private String name;
	
	public static final EolCollectionType
		Collection = new EolCollectionType("Collection"),
		Bag = new EolCollectionType("Bag"),
		Sequence = new EolCollectionType("Sequence"),
		Set = new EolCollectionType("Set"),
		OrderedSet = new EolCollectionType("OrderedSet"),
		ConcurrentBag = new EolCollectionType("ConcurrentBag"),
		ConcurrentSet = new EolCollectionType("ConcurrentSet");
	
	public EolCollectionType(String name) {
		this.name = name;
	}
	
	public EolCollectionType(String name, EolType contentType) {
		this(name);
		this.contentType = contentType;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public boolean isBag() {
		return "Bag".equals(getName());
	}
	
	public boolean isSequence() {
		return "Sequence".equals(getName());
	}
	
	public boolean isSet() {
		return "Set".equals(getName());
	}
	
	public boolean isOrderedSet() {
		return "OrderedSet".equals(getName());
	}
	
	public boolean isCollection() {
		return "Collection".equals(getName());
	}
	
	/**
	 * 
	 * @return
	 * @since 1.6
	 */
	public boolean isConcurrentBag() {
		return "ConcurrentBag".equals(getName());
	}
	
	/**
	 * 
	 * @return
	 * @since 1.6
	 */
	public boolean isConcurrentSet() {
		return "ConcurrentSet".equals(getName());
	}
	
	public EolType getContentType() {
		return contentType;
	}
	
	public void setContentType(EolType contentType) {
		this.contentType = contentType;
	}
	
	@Override
	public String toString() {
		return this.getName() + "<" + this.getContentType() + ">";
	}
	
	/*
	If B is a sub-type of A, then the parent types of Sequence(B) are:
		- Collection(B)
		- Sequence(A) [This is not supported yet by the implementation below]
	 */
	@Override
	public EolType getParentType() {
		if (this.isBag() || this.isSet() || this.isOrderedSet() || this.isSequence())
			return new EolCollectionType("Collection", this.getContentType());
		else {
			if (!(this.getContentType() instanceof EolAnyType))
				return new EolCollectionType("Collection", EolAnyType.Instance);
			else
				return EolAnyType.Instance;
		}
	}
	
	@Override
	public List<EolType> getParentTypes() {
		List<EolType> parentTypes = new ArrayList<EolType>();
		if (this.isBag() || this.isSet() || this.isOrderedSet() || this.isSequence())
			parentTypes.add(new EolCollectionType("Collection", this.getContentType()));
		else
			parentTypes.add(EolAnyType.Instance);

		if (!(this.getContentType() instanceof EolAnyType))
			parentTypes.addAll(this.getContentType().getParentTypes().stream()
					.map(e -> new EolCollectionType(this.getName(), e)).collect(Collectors.toList()));
		return parentTypes;
	}
}
