package org.eclipse.epsilon.eol.analyse.operations.declarative;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DeclarativeOperationTypeProvider {
	Class<? extends IDeclarativeOperationTypeProvider> value();
}
