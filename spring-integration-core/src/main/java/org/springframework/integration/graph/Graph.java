/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import java.util.Collection;
import java.util.Map;

/**
 * This object can be exposed, for example, as a JSON object over
 * HTTP.
 *
 * @author Andy Clement
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class Graph {

	private final Map<String, Object> contentDescriptor;

	private final Collection<IntegrationNode> nodes;

	private final Collection<LinkNode> links;

	public Graph(Map<String, Object> descriptor, Collection<IntegrationNode> nodes, Collection<LinkNode> links) {
		this.contentDescriptor = descriptor;
		this.nodes = nodes;
		this.links = links;
	}

	public Map<String, Object> getContentDescriptor() {
		return this.contentDescriptor;
	}

	public Collection<IntegrationNode> getNodes() {
		return this.nodes;
	}

	public Collection<LinkNode> getLinks() {
		return this.links;
	}

}
