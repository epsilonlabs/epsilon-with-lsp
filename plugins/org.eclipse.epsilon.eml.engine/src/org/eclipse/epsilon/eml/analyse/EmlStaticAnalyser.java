package org.eclipse.epsilon.eml.analyse;

import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eml.IEmlModule;
import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.erl.analyse.ErlStaticAnalyser;

public class EmlStaticAnalyser extends ErlStaticAnalyser {

	public EmlStaticAnalyser(IModelFactory modelFactory) {
		super(modelFactory);
	}

	public EmlStaticAnalyser() {
	}

	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		if (!(imodule instanceof IEmlModule))
			return Collections.emptyList();

		return super.validate(imodule);
	}
}
