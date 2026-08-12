package org.eclipse.epsilon.eol.m3;

import java.util.Collections;
import java.util.List;

public interface IEnum extends IDataType {
	boolean isValidEnumLiteral(String literal);

	default List<String> getLiterals() {
		return Collections.emptyList();
	}
}
