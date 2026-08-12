package org.eclipse.epsilon.eol.m3;

import java.util.Collections;
import java.util.List;

public interface IMetamodel {
	
	List<String> getWarnings();

	List<String> getErrors();

	default IMetaType getMetaType(String name) {
		IMetaClass metaClass = getMetaClass(name);
		if (metaClass != null) {
			return metaClass;
		}
		return getDataType(name);
	}

	default IDataType getDataType(String name) {
		for (IDataType dataType : getDataTypes()) {
			if (dataType.getName().equals(name)) {
				return dataType;
			}
		}
		return null;
	}

	IMetaClass getMetaClass(String name);

	List<Package> getSubPackages();

	List<IMetaClass> getTypes();

	default List<? extends IDataType> getDataTypes() {
		return Collections.emptyList();
	}
}
