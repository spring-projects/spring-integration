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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.context.ExpressionCapable;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * Base class for all nodes.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public abstract class IntegrationNode {

	private final int nodeId;

	private final String nodeName;

	private final String componentType;

	@Nullable
	private final IntegrationPatternType integrationPatternType;

	@Nullable
	private final IntegrationPatternType.IntegrationPatternCategory integrationPatternCategory;

	private final Map<String, Object> properties = new HashMap<>();

	private final Map<String, Object> unmodifiableProperties = Collections.unmodifiableMap(this.properties);

	protected IntegrationNode(int nodeId, String name, Object nodeObject) {
		this.nodeId = nodeId;
		this.nodeName = name;
		this.componentType =
				nodeObject instanceof NamedComponent
						? ((NamedComponent) nodeObject).getComponentType()
						: nodeObject.getClass().getSimpleName();
		if (nodeObject instanceof ExpressionCapable) {
			Expression expression = ((ExpressionCapable) nodeObject).getExpression();
			if (expression != null) {
				this.properties.put("expression", expression.getExpressionString());
			}
		}

		IntegrationPatternType patternType = null;

		if (nodeObject instanceof IntegrationPattern) {
			patternType = ((IntegrationPattern) nodeObject).getIntegrationPatternType(); // NOSONAR
		}
		else if (nodeObject instanceof MessageHandler) {
			patternType = IntegrationPatternType.service_activator;
		}
		else if (nodeObject instanceof MessageChannel) {
			patternType = IntegrationPatternType.message_channel;
		}

		this.integrationPatternType = patternType;
		if (this.integrationPatternType != null) {
			this.integrationPatternCategory = this.integrationPatternType.getPatternCategory();
		}
		else {
			this.integrationPatternCategory = null;
		}
	}

	public int getNodeId() {
		return this.nodeId;
	}

	public String getName() {
		return this.nodeName;
	}

	public final String getComponentType() {
		return this.componentType;
	}

	@Nullable
	public IntegrationPatternType getIntegrationPatternType() {
		return this.integrationPatternType;
	}

	@Nullable
	public IntegrationPatternType.IntegrationPatternCategory getIntegrationPatternCategory() {
		return this.integrationPatternCategory;
	}

	public Map<String, Object> getProperties() {
		return this.unmodifiableProperties;
	}

	/**
	 * Add extra property to the node.
	 * @param name the name for property
	 * @param value the value of the property
	 * @since 5.1
	 */
	public void addProperty(String name, Object value) {
		Assert.hasText(name, "'name' must not be null");
		this.properties.put(name, value);
	}

	/**
	 * Add extra property to the node.
	 * @param props additional properties to add
	 * @since 5.1
	 */
	public void addProperties(@Nullable Map<String, Object> props) {
		if (props != null) {
			this.properties.putAll(props);
		}
	}

}
