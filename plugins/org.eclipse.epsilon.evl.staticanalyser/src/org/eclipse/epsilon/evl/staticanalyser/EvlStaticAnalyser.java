package org.eclipse.epsilon.evl.staticanalyser;

import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.staticanalyser.execute.context.Variable;
import org.eclipse.epsilon.eol.EolModule;
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
		
		if (constraintContext.getGuardBlock() != null)
			constraintContext.getGuardBlock().accept(this);

		for (Constraint c : constraintContext.getConstraints())
			c.accept(this);
		
		context.getFrameStack().remove("self");
	}

	@Override
	public void visit(Constraint constraint) {
		if (constraint.getGuardBlock() != null)
			constraint.getGuardBlock().accept(this);

		if (constraint.getCheckBlock() != null)
			constraint.getCheckBlock().accept(this);

		if (constraint.getMessageBlock() != null)
			constraint.getMessageBlock().accept(this);

		for (Fix f : constraint.getFixes()) {
			if (f.getBodyBlock() != null)
				f.getBodyBlock().accept(this);
			if (f.getGuardBlock() != null)
				f.getGuardBlock().accept(this);
			if (f.getTitleBlock() != null)
				f.getTitleBlock().accept(this);
		}
	}

	@Override
	public void visit(Fix fix) {
		// TODO Auto-generated method stub
	}
}
