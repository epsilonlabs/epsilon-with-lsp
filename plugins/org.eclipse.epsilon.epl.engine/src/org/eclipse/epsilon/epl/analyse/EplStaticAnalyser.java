package org.eclipse.epsilon.epl.analyse;

import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.epl.IEplModule;
import org.eclipse.epsilon.erl.analyse.ErlStaticAnalyser;

public class EplStaticAnalyser extends ErlStaticAnalyser {

	public EplStaticAnalyser(IModelFactory modelFactory) {
		super(modelFactory);
	}

	public EplStaticAnalyser() {
	}

	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		if (!(imodule instanceof IEplModule))
			return Collections.emptyList();

		return super.validate(imodule);
	}
}
