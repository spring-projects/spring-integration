/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.jmx;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.gateway.SimpleMessageMapper;
import org.springframework.integration.message.InboundMessageMapper;
import org.springframework.integration.message.MessageSource;
import org.springframework.jmx.support.ObjectNameManager;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@SuppressWarnings("unchecked")
public class AttributePollingMessageSource implements MessageSource {

	private volatile ObjectName objectName;

	private volatile String attributeName;

	private volatile MBeanServer server;

	private volatile InboundMessageMapper<Object> mapper = new SimpleMessageMapper();


	public void setObjectName(String objectName) {
		try {
			this.objectName = ObjectNameManager.getInstance(objectName);
		}
		catch (MalformedObjectNameException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public void setServer(MBeanServer server) {
		this.server = server;
	}

	public Message<?> receive() {
		try {
			Object value = this.server.getAttribute(this.objectName, this.attributeName);
			return this.mapper.toMessage(value);
		}
		catch (Exception e) {
			throw new MessagingException("failed to retrieve JMX attribute '"
					+ this.attributeName + "' on MBean [" + this.objectName + "]", e);
		}
	}

}
