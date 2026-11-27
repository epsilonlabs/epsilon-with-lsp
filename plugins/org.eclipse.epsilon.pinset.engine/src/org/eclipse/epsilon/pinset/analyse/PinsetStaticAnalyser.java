package org.eclipse.epsilon.pinset.analyse;

import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.erl.analyse.ErlStaticAnalyser;
import org.eclipse.epsilon.pinset.PinsetModule;

public class PinsetStaticAnalyser extends ErlStaticAnalyser {

	public PinsetStaticAnalyser(IModelFactory modelFactory) {
		super(modelFactory);
	}

	public PinsetStaticAnalyser() {
	}

	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		if (!(imodule instanceof PinsetModule))
			return Collections.emptyList();

		return super.validate(imodule);
	}
}
