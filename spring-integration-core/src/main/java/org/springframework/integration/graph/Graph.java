/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
