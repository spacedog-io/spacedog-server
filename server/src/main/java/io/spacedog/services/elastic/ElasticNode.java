package io.spacedog.services.elastic;

import java.util.Arrays;

import org.elasticsearch.env.Environment;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;

public class ElasticNode extends Node {

	@SafeVarargs
	public ElasticNode(Environment environment, Class<? extends Plugin>... plugins) {
		super(environment, Arrays.asList(plugins));
	}
}
