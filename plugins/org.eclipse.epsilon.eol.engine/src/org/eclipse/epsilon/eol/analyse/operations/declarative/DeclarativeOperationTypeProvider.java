package org.eclipse.epsilon.eol.analyse.operations.declarative;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DeclarativeOperationTypeProvider {
	Class<? extends IDeclarativeOperationTypeProvider> value();
}
