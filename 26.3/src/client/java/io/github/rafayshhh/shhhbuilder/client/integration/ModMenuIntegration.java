package io.github.rafayshhh.shhhbuilder.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.rafayshhh.shhhbuilder.client.gui.ShhhBuilderConfigScreen;

public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ShhhBuilderConfigScreen::new;
	}
}
