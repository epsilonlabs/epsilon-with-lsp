package org.eclipse.epsilon.eol.staticanalyser.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EolUnionType extends EolType {
	
	public Set<EolType> containedTypes = new HashSet<EolType>();
	
	public EolUnionType(EolType... eolTypes) {
		for (EolType type : eolTypes) {
			containedTypes.add(type);
		}
	}
	
	public EolUnionType(Collection<? extends EolType> eolTypes) {
		containedTypes.addAll(eolTypes);
	}
	
	@Override
	public String getName() {
		return containedTypes.stream().map(t -> t.getName()).collect(Collectors.joining("|"));
	}
	
	@Override
	public String toString() {
		return containedTypes.stream().map(t -> t.toString()).collect(Collectors.joining("|"));
	}
	
	@Override
	public List<EolType> getParentTypes(){
		return new ArrayList<EolType>(containedTypes); 
	}
	
	@Override
	public List<EolType> getChildrenTypes(){
		List<EolType> children = new ArrayList<EolType>();
		children.addAll(containedTypes);
		return children;
	}
}
