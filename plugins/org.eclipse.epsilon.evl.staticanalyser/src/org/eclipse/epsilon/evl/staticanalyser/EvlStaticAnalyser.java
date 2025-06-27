package org.eclipse.epsilon.evl.staticanalyser;

import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.eol.staticanalyser.execute.context.Variable;
import org.eclipse.epsilon.eol.staticanalyser.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.dom.ExecutableBlock;
import org.eclipse.epsilon.eol.staticanalyser.EolStaticAnalyser;
import org.eclipse.epsilon.eol.staticanalyser.IModelFactory;
import org.eclipse.epsilon.erl.dom.Post;
import org.eclipse.epsilon.erl.dom.Pre;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.epsilon.evl.dom.Constraint;
import org.eclipse.epsilon.evl.dom.ConstraintContext;
import org.eclipse.epsilon.evl.dom.Fix;
import org.eclipse.epsilon.evl.dom.IEvlVisitor;

public class EvlStaticAnalyser extends EolStaticAnalyser implements IEvlVisitor {
	
	public EvlStaticAnalyser(IModelFactory modelFactory) {
		super(modelFactory);
	}

	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		if (!(imodule instanceof EvlModule))
			return Collections.emptyList();

		this.module = (EolModule) imodule;
		EvlModule evlModule = (EvlModule) imodule;

		super.preValidate(evlModule);
		for (Pre pre : evlModule.getPre()) {
			pre.accept(this);
		}
		super.mainValidate();

		for (ConstraintContext cc : evlModule.getConstraintContexts()) {
			cc.accept(this);
		}
		for (Post post : evlModule.getPost()) {
			post.accept(this);
		}
		super.postValidate();

		return errors;
	}

	@Override
	public void visit(Post post) {
		post.getBody().accept(this);
	}

	@Override
	public void visit(Pre pre) {
		pre.getBody().accept(this);
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
		checkBoolean(constraint.getGuardBlock());

		checkBoolean(constraint.getCheckBlock());

		checkString(constraint.getMessageBlock());

		for (Fix f : constraint.getFixes())
			f.accept(this);
	}
	
	private void checkBoolean(ExecutableBlock<Boolean> block) {
		if (block != null) {
			block.accept(this);
			if (!getResolvedType(block).equals(EolPrimitiveType.Boolean)) {
				errors.add(new ModuleMarker(block,
						"Block must return Boolean ", Severity.Error));
			}
		}
	}
	
	private void checkString(ExecutableBlock<String> block) {
		if (block != null) {
			block.accept(this);
			if (!getResolvedType(block).equals(EolPrimitiveType.String)) {
				EolType type = getResolvedType(block);
				try {
					Class<?> args[] = null;
					type.getClazz().getMethod("toString", args);
				} catch (NoSuchMethodException e) {
					errors.add(new ModuleMarker(block,
							"Block must return String ", Severity.Error));
				}
			}
		}
	}

	@Override
	public void visit(Fix fix) {
		if (fix.getBodyBlock() != null)
			fix.getBodyBlock().accept(this);
		checkBoolean(fix.getGuardBlock());
		checkString(fix.getTitleBlock());
	}
}
