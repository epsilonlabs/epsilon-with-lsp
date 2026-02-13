package org.eclipse.epsilon.eol.execute.operations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TypeCalculator {
	Class<? extends ITypeCalculator> klass();
}
