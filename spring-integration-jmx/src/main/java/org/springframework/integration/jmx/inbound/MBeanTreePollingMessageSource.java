/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.integration.jmx.inbound;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.integration.core.MessageSource} implementation that
 * retrieves a snapshot of a filtered subset of the MBean tree.
 *
 * @author Stuart Williams
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 7.0
 *
 */
public class MBeanTreePollingMessageSource extends AbstractMessageSource<Object> {

	@SuppressWarnings("NullAway.Init")
	private MBeanServerConnection server;

	private @Nullable ObjectName queryName = null;

	private QueryExp queryExpression = ObjectName.WILDCARD;

	private final MBeanObjectConverter converter;

	/**
	 * @param converter The converter.
	 */
	public MBeanTreePollingMessageSource(MBeanObjectConverter converter) {
		this.converter = converter;
	}

	/**
	 * Provide the MBeanServer where the JMX MBean has been registered.
	 * @param server The MBean server connection.
	 */
	public void setServer(MBeanServerConnection server) {
		this.server = server;
	}

	/**
	 * @param queryName The query name.
	 */
	public void setQueryName(String queryName) {
		Assert.notNull(queryName, "'queryName' must not be null");
		try {
			setQueryNameReference(ObjectName.getInstance(queryName));
		}
		catch (MalformedObjectNameException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * @param queryName The query name.
	 */
	public void setQueryNameReference(ObjectName queryName) {
		this.queryName = queryName;
	}

	/**
	 * @param queryExpression The query expression.
	 */
	public void setQueryExpression(String queryExpression) {
		Assert.notNull(queryExpression, "'queryExpression' must not be null");
		try {
			setQueryExpressionReference(ObjectName.getInstance(queryExpression));
		}
		catch (MalformedObjectNameException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * @param queryExpression The query expression.
	 */
	public void setQueryExpressionReference(QueryExp queryExpression) {
		this.queryExpression = queryExpression;
	}

	@Override
	public String getComponentType() {
		return "jmx:tree-polling-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.notNull(this.server, "MBeanServer is required");
	}

	@Override
	protected @Nullable Object doReceive() {
		try {
			Map<String, Object> beans = new HashMap<>();
			Set<ObjectInstance> results = this.server.queryMBeans(this.queryName, this.queryExpression);

			for (ObjectInstance instance : results) {
				Object result = this.converter.convert(this.server, instance);
				beans.put(instance.getObjectName().getCanonicalName(), result);
			}

			return beans;

		}
		catch (Exception ex) {
			throw new MessagingException("Failed to retrieve tree snapshot", ex);
		}
	}

}
