/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.support.management.graph;

import java.util.Collection;

/**
 * This object can be exposed, for example, as a JSON object over
 * HTTP.
 *
 * @author Andy Clement
 * @author Gary Russell
 * @since 4.3
 *
 */
public class Graph {

	private final Collection<IntegrationNode> nodes;

	private final Collection<LinkNode> links;

	public Graph(Collection<IntegrationNode> nodes, Collection<LinkNode> links) {
		this.nodes = nodes;
		this.links = links;
	}

	public Collection<IntegrationNode> getNodes() {
		return this.nodes;
	}

	public Collection<LinkNode> getLinks() {
		return this.links;
	}

}
