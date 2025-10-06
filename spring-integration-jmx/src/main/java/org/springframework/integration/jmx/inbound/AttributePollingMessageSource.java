/*
 * Copyright 2002-present the original author or authors.
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

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.integration.core.MessageSource} implementation that
 * retrieves the current value of a JMX attribute each time {@link #receive()} is invoked.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class AttributePollingMessageSource extends AbstractMessageSource<Object> {

	@SuppressWarnings("NullAway.Init")
	private ObjectName objectName;

	@SuppressWarnings("NullAway.Init")
	private String attributeName;

	@SuppressWarnings("NullAway.Init")
	private MBeanServerConnection server;

	/**
	 * Provide the MBeanServer where the JMX MBean has been registered.
	 * @param server The MBean server connection.
	 */
	public void setServer(MBeanServerConnection server) {
		this.server = server;
	}

	/**
	 * Specify the String value of the JMX MBean's {@link ObjectName}.
	 * @param objectName The object name.
	 */
	public void setObjectName(String objectName) {
		try {
			this.objectName = ObjectNameManager.getInstance(objectName);
		}
		catch (MalformedObjectNameException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * Specify the name of the attribute to be retrieved.
	 * @param attributeName The attribute name.
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	@Override
	public String getComponentType() {
		return "jmx:attribute-polling-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.notNull(this.server, "MBeanServer is required");
		Assert.notNull(this.objectName, "object name is required");
		Assert.notNull(this.attributeName, "attribute name is required");
	}

	/**
	 * Retrieves the JMX attribute value.
	 */
	@Override
	protected @Nullable Object doReceive() {
		try {
			return this.server.getAttribute(this.objectName, this.attributeName);
		}
		catch (Exception ex) {
			throw new MessagingException("failed to retrieve JMX attribute '"
					+ this.attributeName + "' on MBean [" + this.objectName + "]", ex);
		}
	}

}
