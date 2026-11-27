package org.eclipse.epsilon.emg.analyse;

import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.emg.IEmgModule;
import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.erl.analyse.ErlStaticAnalyser;

public class EmgStaticAnalyser extends ErlStaticAnalyser {

	public EmgStaticAnalyser(IModelFactory modelFactory) {
		super(modelFactory);
	}

	public EmgStaticAnalyser() {
	}

	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		if (!(imodule instanceof IEmgModule))
			return Collections.emptyList();

		return super.validate(imodule);
	}
}
