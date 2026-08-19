package org.eclipse.epsilon.eol.analyse;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.UnknownModel;

public class DefaultModelFactory implements IModelFactory {

	protected final Map<String, Supplier<? extends IModel>> modelSuppliers = new HashMap<>();

	public DefaultModelFactory() {
		registerModel("Unknown", UnknownModel::new);
	}

	public DefaultModelFactory registerModel(String driver, Supplier<? extends IModel> modelSupplier) {
		modelSuppliers.put(
			Objects.requireNonNull(driver, "driver").toLowerCase(Locale.ROOT),
			Objects.requireNonNull(modelSupplier, "modelSupplier"));
		return this;
	}

	@Override
	public IModel createModel(String driver) {
		Supplier<? extends IModel> modelSupplier = modelSuppliers.get(
			Objects.requireNonNull(driver, "driver").toLowerCase(Locale.ROOT));
		return modelSupplier != null ? modelSupplier.get() : null;
	}
}
