package org.eclipse.epsilon.eol.analyse.operations.validation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface OperationCallValidator {
	Class<? extends IOperationCallValidator> value();
}
