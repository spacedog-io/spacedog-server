package io.spacedog.services;

import java.util.Collection;

import org.elasticsearch.Version;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.Plugin;

public class SpaceNode extends Node {

	protected SpaceNode(Settings settings, Collection<Class<? extends Plugin>> classpathPlugins) {
		super(InternalSettingsPreparer.prepareEnvironment(settings, null), //
				Version.CURRENT, classpathPlugins);
	}

}
