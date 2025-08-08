/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

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
 * @since 2.0
 */
public class AttributePollingMessageSource extends AbstractMessageSource<Object> {

	private volatile ObjectName objectName;

	private volatile String attributeName;

	private volatile MBeanServerConnection server;

	/**
	 * Provide the MBeanServer where the JMX MBean has been registered.
	 *
	 * @param server The MBean server connection.
	 */
	public void setServer(MBeanServerConnection server) {
		this.server = server;
	}

	/**
	 * Specify the String value of the JMX MBean's {@link ObjectName}.
	 *
	 * @param objectName The object name.
	 */
	public void setObjectName(String objectName) {
		try {
			this.objectName = ObjectNameManager.getInstance(objectName);
		}
		catch (MalformedObjectNameException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Specify the name of the attribute to be retrieved.
	 *
	 * @param attributeName The attribute name.
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	@Override
	public String getComponentType() {
		return "jmx:attribute-polling-channel-adapter";
	}

	/**
	 * Retrieves the JMX attribute value.
	 */
	@Override
	protected Object doReceive() {
		Assert.notNull(this.server, "MBeanServer is required");
		Assert.notNull(this.objectName, "object name is required");
		Assert.notNull(this.attributeName, "attribute name is required");
		try {
			return this.server.getAttribute(this.objectName, this.attributeName);
		}
		catch (Exception e) {
			throw new MessagingException("failed to retrieve JMX attribute '"
					+ this.attributeName + "' on MBean [" + this.objectName + "]", e);
		}
	}

}
