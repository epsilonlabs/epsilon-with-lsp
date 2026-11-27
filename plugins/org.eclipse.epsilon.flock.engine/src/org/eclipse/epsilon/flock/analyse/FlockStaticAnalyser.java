package org.eclipse.epsilon.flock.analyse;

import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.erl.analyse.ErlStaticAnalyser;
import org.eclipse.epsilon.flock.IFlockModule;

public class FlockStaticAnalyser extends ErlStaticAnalyser {

	public FlockStaticAnalyser(IModelFactory modelFactory) {
		super(modelFactory);
	}

	public FlockStaticAnalyser() {
	}

	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		if (!(imodule instanceof IFlockModule))
			return Collections.emptyList();

		return super.validate(imodule);
	}
}
