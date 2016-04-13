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

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.support.context.NamedComponent;

/**
 * Base class for all endpoints.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public abstract class IntegrationNode {

	private static final AtomicInteger nodeId = new AtomicInteger();

	private final int myNodeId;

	private final String name;

	private final String output;

	private final String input;

	private final Stats stats;

	private final String componentType;

	private final String componentName;

	protected IntegrationNode(String name, Object nodeObject, Stats stats) {
		this(name, nodeObject, null, null, stats);
	}

	protected IntegrationNode(String name, Object nodeObject, String output, String input, Stats stats) {
		this.myNodeId = nodeId.incrementAndGet();
		this.name = name;
		this.output = output;
		this.input = input;
		this.componentType = nodeObject instanceof NamedComponent ? ((NamedComponent) nodeObject).getComponentType()
				: "unknown";
		this.componentName = nodeObject instanceof NamedComponent ? ((NamedComponent) nodeObject).getComponentName()
				: "unknown";
		this.stats = stats;
	}

	public int getNodeId() {
		return this.myNodeId;
	}

	public String getName() {
		return this.name;
	}

	public String getOutput() {
		return this.output;
	}

	public String getInput() {
		return this.input;
	}

	public String getComponentType() {
		return this.componentType;
	}

	public String getComponentName() {
		return this.componentName;
	}

	public Stats getStats() {
		return this.stats.isAvailable() ? this.stats : null;
	}

	public static class Stats {

		public boolean isAvailable() {
			return false;
		}

	}

}
