package org.eclipse.epsilon.eol.m3;

import java.util.List;

public interface IMetamodel {
	
	List<String> getWarnings();

	List<String> getErrors();

	IMetaClass getMetaClass(String name);

	List<Package> getSubPackages();

	List<MetaClass> getTypes();
}
