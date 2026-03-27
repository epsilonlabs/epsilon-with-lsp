package org.eclipse.epsilon.eol.staticanalyser.types;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EolNativeType extends EolType {

	private Class<?> javaClass;
	private String className;
	
	public static final EolNativeType Number = new EolNativeType(java.lang.Number.class);
	
	public EolNativeType(Class<?> javaClass) {
		this.javaClass = javaClass;
		this.className = javaClass.getName();
	}
	
	public EolNativeType(String className) {
		this.javaClass = null;
		this.className = className;
	}
	
	@Override
	public String getName() {
		return "Native";
	}
	
	@Override
	public Class<?> getClazz(){
		return javaClass;
	}
	
	@Override
	public String toString() {
		return "Native<" + className +">";
	}
	
	@Override
	public List<EolType> getParentTypes() {
		if (javaClass == null || javaClass == Object.class) {
			return Collections.emptyList();
		}else {
			//TODO Also support Interfaces
			if(javaClass.getSuperclass() != null) {
				return Arrays.asList(new EolNativeType(javaClass.getSuperclass()));
			}else {
				return Collections.emptyList();
			}
		}
	}

}
