/*
 * Copyright 2016-2020 the original author or authors.
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

/**
 * Base class for all endpoints.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public abstract class EndpointNode extends IntegrationNode {

	private final String output;

	protected EndpointNode(int nodeId, String name, Object nodeObject, String output) {
		super(nodeId, name, nodeObject);
		this.output = output;
	}

	public String getOutput() {
		return this.output;
	}

}
