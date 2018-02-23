package io.spacedog.server;

import java.util.Arrays;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;

public class ElasticNode extends Node {

	@SafeVarargs
	protected ElasticNode(Settings settings, Class<? extends Plugin>... plugins) {
		super(InternalSettingsPreparer.prepareEnvironment(settings, null), //
				Arrays.asList(plugins));
	}
}
