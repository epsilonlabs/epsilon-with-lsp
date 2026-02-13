package org.eclipse.epsilon.eol.execute.operations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface MethodTypeCalculator {
	Class<? extends IMethodTypeCalculator> klass();
}
