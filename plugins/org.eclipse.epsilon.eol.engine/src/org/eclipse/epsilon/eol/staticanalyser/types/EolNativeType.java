package org.eclipse.epsilon.eol.staticanalyser.types;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EolNativeType extends EolType {

	private Class<?> javaClass;
	
	public static final EolNativeType Number = new EolNativeType(java.lang.Number.class);
	
	public EolNativeType(Class<?> javaClass) {
		this.javaClass = javaClass;
	}
	
	@Override
	public String getName() {
		return "Native";
	}
	
	@Override
	public String toString() {
		return "Native<" + javaClass.getName() +">";
	}
	
	@Override
	public List<EolType> getParentTypes() {
		if (javaClass == Object.class) {
			return Collections.emptyList();
		}else {
			//TODO Also support Interfaces
			return Arrays.asList(new EolNativeType(javaClass.getSuperclass()));
		}
	}

}
