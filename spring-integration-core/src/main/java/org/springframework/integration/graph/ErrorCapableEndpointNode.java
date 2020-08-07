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
 * Represents nodes that can natively handle errors.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class ErrorCapableEndpointNode extends EndpointNode implements ErrorCapableNode {

	private final String errors;

	protected ErrorCapableEndpointNode(int nodeId, String name, Object nodeObject, String output, String errors) {
		super(nodeId, name, nodeObject, output);
		this.errors = errors;
	}

	@Override
	public String getErrors() {
		return this.errors;
	}

}
