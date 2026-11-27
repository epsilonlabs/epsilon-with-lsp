package org.eclipse.epsilon.ecl.analyse;

import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.ecl.EclModule;
import org.eclipse.epsilon.ecl.dom.IEclVisitor;
import org.eclipse.epsilon.ecl.dom.MatchRule;
import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.eol.dom.ExecutableBlock;
import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.dom.StatementBlock;
import org.eclipse.epsilon.eol.execute.context.FrameType;
import org.eclipse.epsilon.erl.IErlModule;
import org.eclipse.epsilon.erl.analyse.ErlStaticAnalyser;

public class EclStaticAnalyser extends ErlStaticAnalyser implements IEclVisitor {

	public EclStaticAnalyser(IModelFactory modelFactory) {
		super(modelFactory);
	}

	public EclStaticAnalyser() {
	}

	@Override
	public void visit(MatchRule matchRule) {
		matchRule.getLeftParameter().accept(this);
		matchRule.getRightParameter().accept(this);
		if (matchRule.getGuardBlock() != null)
			visit(matchRule.getGuardBlock());
		
		if (matchRule.getDoBlock() != null)
			visit(matchRule.getDoBlock());
		
		if (matchRule.getCompareBlock() != null)
			visit(matchRule.getCompareBlock());
		
		if (matchRule.getLeftDomainBlock() != null)
			visit(matchRule.getLeftDomainBlock());
		
		if (matchRule.getRightDomainBlock() != null)
			visit(matchRule.getRightDomainBlock());
		
	}
	
	public void visit(ExecutableBlock<?> ex) {
		if(ex != null) {
			if(ex.getBody() instanceof StatementBlock) {
				StatementBlock guardBody = ((StatementBlock) ex.getBody());
				guardBody.accept(this);
			}
			else
				((Expression)ex.getBody()).accept(this);
		}
	}

	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		if (!(imodule instanceof EclModule))
			return Collections.emptyList();

		List<ModuleMarker> result = super.validate(imodule);
		this.module.getData().put("staticAnalyser", this);
		return result;
	}

	@Override
	protected void validateRules(IErlModule module) {
		for (MatchRule tr : ((EclModule) module).getDeclaredMatchRules()) {
			this.getContext().getFrameStack().enterLocal(FrameType.UNPROTECTED, tr);
			tr.accept(this);
			this.getContext().getFrameStack().leaveLocal(tr);
		}
	}

}
