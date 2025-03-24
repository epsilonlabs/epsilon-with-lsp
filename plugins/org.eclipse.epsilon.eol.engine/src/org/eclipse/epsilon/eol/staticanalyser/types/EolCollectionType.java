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

import org.eclipse.epsilon.eol.types.EolBag;
import org.eclipse.epsilon.eol.types.EolCollection;
import org.eclipse.epsilon.eol.types.EolOrderedSet;
import org.eclipse.epsilon.eol.types.EolSequence;
import org.eclipse.epsilon.eol.types.EolSet;
import org.eclipse.epsilon.eol.types.concurrent.EolConcurrentBag;
import org.eclipse.epsilon.eol.types.concurrent.EolConcurrentSet;

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
		switch(name) {
		case "Collection":
			setClazz(EolCollection.class);
			break;
		case "Bag":
			setClazz(EolBag.class);
			break;
		case "Sequence":
			setClazz(EolSequence.class);
			break;
		case "Set":
			setClazz(EolSet.class);
			break;
		case "OrderedSet":
			setClazz(EolOrderedSet.class);
			break;
		case "ConcurrentBag":
			setClazz(EolConcurrentBag.class);
			break;
		case "ConcurrentSet":
			setClazz(EolConcurrentSet.class);
			break;
		}
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
				return null;
		}
	}
	
	@Override
	public List<EolType> getParentTypes() {
		List<EolType> parentTypes = new ArrayList<EolType>();
		if (this.isBag() || this.isSet() || this.isOrderedSet() || this.isSequence())
			parentTypes.add(new EolCollectionType("Collection"));
		return parentTypes;
	}
	
	public boolean isAssignableTo(EolType targetType) {
		if (targetType.equals(EolAnyType.Instance)){
			return true;
		}
		if (!(targetType instanceof EolCollectionType)) {
			return false;
		}
		EolType targetContentType = ((EolCollectionType) targetType).getContentType();
		EolType newThis = new EolCollectionType(this.getName());
		EolCollectionType newTargetType = new EolCollectionType(targetType.getName());
		
		
		if(!newTargetType.isAncestorOf(newThis)) {
			return false;
		}
		else {
			if(targetContentType instanceof EolAnyType || this.getContentType() instanceof EolAnyType) {
				return true;
			}
			if(targetContentType.equals(this.getContentType())) {
				return true;
			}
		}

		return false;
	}
}
