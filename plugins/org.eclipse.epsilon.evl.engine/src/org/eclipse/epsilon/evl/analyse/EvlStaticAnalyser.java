package org.eclipse.epsilon.evl.analyse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.eol.analyse.SimpleOperation;
import org.eclipse.epsilon.eol.execute.context.FrameType;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.dom.ExecutableBlock;
import org.eclipse.epsilon.eol.dom.ReturnStatement;
import org.eclipse.epsilon.erl.IErlModule;
import org.eclipse.epsilon.erl.analyse.ErlStaticAnalyser;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.epsilon.evl.dom.Constraint;
import org.eclipse.epsilon.evl.dom.ConstraintContext;
import org.eclipse.epsilon.evl.dom.Fix;
import org.eclipse.epsilon.evl.dom.IEvlVisitor;

public class EvlStaticAnalyser extends ErlStaticAnalyser implements IEvlVisitor {
	
	public EvlStaticAnalyser(IModelFactory modelFactory) {
		super(modelFactory);
	}
	
	public EvlStaticAnalyser() {
	}

	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		if (!(imodule instanceof EvlModule))
			return Collections.emptyList();

		return super.validate(imodule);
	}

	@Override
	protected void registerBuiltins(IErlModule module) {
		// Register EVL built-in operations (satisfies, satisfiesAll, satisfiesOne)
		// These are runtime operations from EvlOperationFactory that the static analyser
		// needs to know about. They can be called on any object with one or more String
		// arguments (constraint names) and return Boolean.
		// Must be added after preValidate() which populates builtinOperations from
		// OperationContributors (it skips population if the list is non-empty).
		for (String opName : Arrays.asList("satisfies", "satisfiesAll", "satisfiesOne")) {
			builtinOperations.add(new SimpleOperation(
				opName,
				EolAnyType.Instance,
				EolPrimitiveType.Boolean,
				Arrays.asList(EolPrimitiveType.String),
				true,
				Optional.empty(),
				Optional.empty()
			));
		}
	}

	@Override
	protected void validateRules(IErlModule module) {
		for (ConstraintContext cc : ((EvlModule) module).getDeclaredConstraintContexts()) {
			cc.accept(this);
		}
	}

	@Override
	public void visit(ConstraintContext constraintContext) {
		constraintContext.getTypeExpression().accept(this);
		context.getFrameStack().put(new Variable("self", getResolvedType(constraintContext.getTypeExpression())));
		
		checkBoolean(constraintContext.getGuardBlock());

		for (Constraint c : constraintContext.getConstraints())
			c.accept(this);
		
		context.getFrameStack().remove("self");
	}

	@Override
	public void visit(Constraint constraint) {
		context.getFrameStack().enterLocal(FrameType.UNPROTECTED, constraint);
		try {
			checkBoolean(constraint.getGuardBlock());

			checkBoolean(constraint.getCheckBlock());

			visitOptional(constraint.getMessageBlock());

			for (Fix f : constraint.getFixes())
				f.accept(this);
		}
		finally {
			context.getFrameStack().leaveLocal(constraint);
		}
	}
	
	/**
	 * Visits an optional block (e.g. a constraint message or a fix title)
	 * without constraining the type of its result.
	 */
	private void visitOptional(ExecutableBlock<?> block) {
		if (block != null) {
			block.accept(this);
		}
	}

	@Override
	public EolType expectedReturnType(ReturnStatement returnStatement) {
		ModuleElement parent = returnStatement.getParent();
		while (!(parent instanceof ExecutableBlock) && parent != null) {
			parent = parent.getParent();
		}
		if (parent instanceof ExecutableBlock) {
			Class<?> expectedResultClass = ((ExecutableBlock<?>)parent).getExpectedResultClass();
			// Values returned by blocks expected to return a String (e.g. messages
			// and titles) are converted to Strings at runtime (see ExecutableBlock)
			if (expectedResultClass == String.class) {
				return EolAnyType.Instance;
			}
			return javaClassToEolType(expectedResultClass);
		} else {
			return super.expectedReturnType(returnStatement);
		}
	}

	@Override
	public void visit(Fix fix) {
		checkBoolean(fix.getGuardBlock());
		visitOptional(fix.getTitleBlock());
		visitOptional(fix.getBodyBlock());
	}
}
