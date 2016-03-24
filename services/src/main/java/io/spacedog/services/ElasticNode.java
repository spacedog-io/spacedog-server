package io.spacedog.services;

import java.util.Arrays;

import org.elasticsearch.Version;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.Plugin;

public class ElasticNode extends Node {

	@SafeVarargs
	protected ElasticNode(Settings settings, Class<? extends Plugin>... plugins) {
		super(InternalSettingsPreparer.prepareEnvironment(settings, null), //
				Version.CURRENT, Arrays.asList(plugins));
	}
}
