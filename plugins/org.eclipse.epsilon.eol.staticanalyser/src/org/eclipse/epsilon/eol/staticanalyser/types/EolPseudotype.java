package org.eclipse.epsilon.eol.staticanalyser.types;

public abstract class EolPseudotype extends EolType {
	
	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public EolType getParentType() {
		return null;
	}
}
