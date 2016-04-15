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

/**
 * Base class for all endpoints.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public abstract class EndpointNode extends IntegrationNode {

	private final String output;

	private final String input;

	protected EndpointNode(int nodeId, String name, Object nodeObject, Stats stats) {
		this(nodeId, name, nodeObject, null, null, stats);
	}

	protected EndpointNode(int nodeId, String name, Object nodeObject, String output, String input, Stats stats) {
		super(nodeId, name, nodeObject, stats);
		this.output = output;
		this.input = input;
	}

	public String getOutput() {
		return this.output;
	}

	public String getInput() {
		return this.input;
	}

}
