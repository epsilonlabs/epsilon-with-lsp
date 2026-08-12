package org.eclipse.epsilon.emc.emf;

import java.util.Objects;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.epsilon.eol.m3.DataType;

public class EmfDataType extends DataType {

	protected EDataType eDataType;

	public EmfDataType(EDataType eDataType, EmfModelMetamodel metamodel) {
		this.eDataType = Objects.requireNonNull(eDataType, "eDataType");
		this.metamodel = metamodel;
	}

	@Override
	public String getName() {
		return eDataType.getName();
	}

	@Override
	public Class<?> getClazz() {
		return eDataType.getInstanceClass();
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof EmfDataType)) {
			return false;
		}
		EmfDataType otherDataType = (EmfDataType) other;
		return Objects.equals(getClassifierIdentity(), otherDataType.getClassifierIdentity())
			&& Objects.equals(metamodel, otherDataType.metamodel);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClassifierIdentity(), metamodel);
	}

	private String getClassifierIdentity() {
		return EcoreUtil.getURI(eDataType).toString();
	}
}
