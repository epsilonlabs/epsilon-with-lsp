package org.eclipse.epsilon.eol.staticanalyser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.types.EolType;

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
	public boolean isType(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isKind(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object createInstance() throws EolRuntimeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object createInstance(List<Object> parameters) throws EolRuntimeException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<EolType> getChildrenTypes(){
		List<EolType> children = new ArrayList<EolType>();
		children.addAll(containedTypes);
		return children;
	}

}
