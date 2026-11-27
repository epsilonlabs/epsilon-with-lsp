package org.eclipse.epsilon.etl.analyse;

import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.eol.dom.ExecutableBlock;
import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.dom.StatementBlock;
import org.eclipse.epsilon.eol.execute.context.FrameType;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.eol.types.EolPrimitiveType;
import org.eclipse.epsilon.erl.IErlModule;
import org.eclipse.epsilon.erl.analyse.ErlStaticAnalyser;
import org.eclipse.epsilon.etl.EtlModule;
import org.eclipse.epsilon.etl.dom.IEtlVisitor;
import org.eclipse.epsilon.etl.dom.TransformationRule;

public class EtlStaticAnalyser extends ErlStaticAnalyser implements IEtlVisitor {

	public EtlStaticAnalyser(IModelFactory modelFactory) {
		super(modelFactory);
	}

	public EtlStaticAnalyser() {
	}

	@Override
	public void visit(TransformationRule transformationRule) {
		transformationRule.getSourceParameter().accept(this);
		transformationRule.getTargetParameters().forEach(t -> t.accept(this));
		ExecutableBlock<Boolean> guard =transformationRule.getGuard();
		if(guard != null) {
			if(guard.getBody() instanceof StatementBlock) {
				StatementBlock guardBody = ((StatementBlock) guard.getBody());
				guardBody.accept(this);
			}
			else
				((Expression)guard.getBody()).accept(this);
		}
		ExecutableBlock<Void> rule =transformationRule.getBody();
		if(rule != null) {
		StatementBlock ruleBody = (StatementBlock) rule.getBody();
		ruleBody.accept(this);
		}
	}

	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		if (!(imodule instanceof EtlModule))
			return Collections.emptyList();

		return super.validate(imodule);
	}

	@Override
	protected void validateRules(IErlModule module) {
		for (TransformationRule tr : ((EtlModule) module).getDeclaredTransformationRules()) {
			this.getContext().getFrameStack().enterLocal(FrameType.UNPROTECTED, tr, new Variable("self", EolPrimitiveType.String));
			tr.accept(this);
			this.getContext().getFrameStack().leaveLocal(tr);
		}
	}

}
