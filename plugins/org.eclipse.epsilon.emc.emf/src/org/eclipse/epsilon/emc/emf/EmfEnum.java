package org.eclipse.epsilon.emc.emf;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.epsilon.eol.m3.IEnum;

public class EmfEnum extends EmfDataType implements IEnum {

	public EmfEnum(EEnum eEnum, EmfModelMetamodel metamodel) {
		super(eEnum, metamodel);
	}

	@Override
	public boolean isValidEnumLiteral(String literal) {
		return getEEnum().getEEnumLiteral(literal) != null;
	}

	@Override
	public List<String> getLiterals() {
		List<String> literals = new ArrayList<String>();
		for (EEnumLiteral literal : getEEnum().getELiterals()) {
			literals.add(literal.getName());
		}
		return literals;
	}

	private EEnum getEEnum() {
		return (EEnum) eDataType;
	}
}
