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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class OperationInvokingHandler extends AbstractReplyProducingMessageHandler implements InitializingBean {

	private volatile ObjectName objectName;

	private volatile MBeanServer server;

	private volatile String defaultOperationName;


	public void setObjectName(String objectName) {
		try {
			this.objectName = ObjectNameManager.getInstance(objectName);
		}
		catch (MalformedObjectNameException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void setServer(MBeanServer server) {
		this.server = server;
	}

	public void setDefaultOperationName(String defaultOperationName) {
		this.defaultOperationName = defaultOperationName; 
	}

	public void afterPropertiesSet() {
		Assert.notNull(this.server, "MBeanServer is required.");
		Assert.notNull(this.objectName, "ObjectName is required.");
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		String operationName = this.resolveOperationName(requestMessage);
		Map<String, Object> paramsFromMessage = this.resolveParameters(requestMessage);
		try {
			MBeanOperationInfo[] opInfoArray = this.server.getMBeanInfo(this.objectName).getOperations();
			for (MBeanOperationInfo opInfo : opInfoArray) {
				if (operationName.equals(opInfo.getName())) {
					MBeanParameterInfo[] paramInfoArray = opInfo.getSignature();
					if (paramInfoArray.length == paramsFromMessage.size()) {
						int index = 0;
						Object values[] = new Object[paramInfoArray.length];
						String signature[] = new String[paramInfoArray.length];
						for (MBeanParameterInfo paramInfo : paramInfoArray) {
							Object value = paramsFromMessage.get(paramInfo.getName());
							if (value != null && value.getClass().getName().equals(paramInfo.getType())) {
								values[index] = value;
								signature[index] = paramInfo.getType();
								index++;
							}
						}
						if (index == paramInfoArray.length) {
							return this.server.invoke(this.objectName, operationName, values, signature);
						}
					}
				}
			}
			throw new MessagingException(requestMessage, "failed to find JMX operation '"
					+ operationName + "' on MBean [" + this.objectName + "]");
		}
		catch (JMException e) {
			throw new MessageHandlingException(requestMessage, "failed to invoke JMX operation '" +
					operationName + "' on MBean [" + this.objectName + "]", e);
		}
	}

	private String resolveOperationName(Message<?> message) {
		String operationName = message.getHeaders().get(JmxHeaders.OPERATION_NAME, String.class);
		if (operationName == null) {
			operationName = this.defaultOperationName;
		}
		Assert.notNull(operationName, "Failed to resolve operation name.");
		return operationName;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> resolveParameters(Message<?> message) {
		Map<String, Object> map = null;
		if (message.getPayload() instanceof Map) {
			map = (Map<String, Object>) message.getPayload();
		}
		else if (message.getPayload() instanceof List) {
			map = this.createParameterMapFromList((List) message.getPayload());
		}
		else if (message.getPayload() != null) {
			map = this.createParameterMapFromList(
					Arrays.asList(ObjectUtils.toObjectArray(message.getPayload())));
		}
		Assert.notNull(map, "Failed to create parameter Map from message.");
		return map;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> createParameterMapFromList(List parameters) {
		Map<String, Object> map = new HashMap<String, Object>();
		for (int i = 0; i < parameters.size(); i++) {
			map.put("p" + (i + 1), parameters.get(i));
		}
		return map;
	}

}
