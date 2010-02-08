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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanInfo;
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
 * As the name suggests this MessageHandler will invoke JMX operation after resolving the 
 * 'objectName', 'operationName' and mapping operation parameters from the message payload. 
 * When operation has multiple parameters they could be provided as List or Map payload.
 * Both 'objectName' and 'operationName' could be provided in two different ways; <br>
 * 1. Setting 'defaultObjectName' via {@link #setDefaultObjectName(String)} and
 * 'defaultOperationName' via {@link #setDefaultOperationName(String)}<br>
 * 2. Supplying values with Message headers such as {@link JmxHeaders#OBJECT_NAME} 
 * and {@link JmxHeaders#OPERATION_NAME}<br>
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class OperationInvokingHandler extends AbstractReplyProducingMessageHandler implements InitializingBean {

	private volatile MBeanServer server;

	private volatile ObjectName defaultObjectName;

	private volatile String defaultOperationName;


	public void setServer(MBeanServer server) {
		this.server = server;
	}

	public void setDefaultObjectName(String defaultObjectName) {
		try {
			if (defaultObjectName != null) {
				this.defaultObjectName = ObjectNameManager.getInstance(defaultObjectName);
			}
		}
		catch (MalformedObjectNameException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void setDefaultOperationName(String defaultOperationName) {
		this.defaultOperationName = defaultOperationName; 
	}

	public void afterPropertiesSet() {
		Assert.notNull(this.server, "MBeanServer is required.");
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		ObjectName objectName = this.resolveObjectName(requestMessage);
		String operationName = this.resolveOperationName(requestMessage);
		Map<String, Object> paramsFromMessage = this.resolveParameters(requestMessage);
		try {
			MBeanInfo mbeanInfo = this.server.getMBeanInfo(objectName);
			MBeanOperationInfo[] opInfoArray = mbeanInfo.getOperations();	
			boolean hasNoArgOption = false;
			for (MBeanOperationInfo opInfo : opInfoArray) {
				if (operationName.equals(opInfo.getName())) {
					MBeanParameterInfo[] paramInfoArray = opInfo.getSignature();
					if (paramInfoArray.length == 0) {
						hasNoArgOption = true;
					}
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
							return this.server.invoke(objectName, operationName, values, signature);
						}
					}
				}
			}
			if (hasNoArgOption) {
				return this.server.invoke(objectName, operationName, null, null);
			}
			throw new MessagingException(requestMessage, "failed to find JMX operation '"
					+ operationName + "' on MBean [" + objectName + "]" + " (implClass:" + mbeanInfo.getClassName()
					+ ")" + " with " + paramsFromMessage.size() + " parameters: "
					+ paramsFromMessage.keySet());
		}
		catch (JMException e) {
			throw new MessageHandlingException(requestMessage, "failed to invoke JMX operation '" +
					operationName + "' on MBean [" + objectName + "]" + " with " + 
					paramsFromMessage.size() + " parameters: " + paramsFromMessage.keySet(), e);
		}
	}

	/**
	 * First checks for the presence of a {@link JmxHeaders#OBJECT_NAME} header,
	 * then falls back to this handler's {@link #defaultObjectName} if available.
	 */
	private ObjectName resolveObjectName(Message<?> message) {
		ObjectName objectName = null;
		Object objectNameHeader = message.getHeaders().get(JmxHeaders.OBJECT_NAME);
		if (objectNameHeader instanceof ObjectName) {
			objectName = (ObjectName) objectNameHeader;
		}
		else if (objectNameHeader instanceof String) {
			try {
				objectName = ObjectNameManager.getInstance(objectNameHeader);
			}
			catch (MalformedObjectNameException e) {
				throw new IllegalArgumentException(e);
			}
		}
		else {
			objectName = this.defaultObjectName;
		}
		Assert.notNull(objectName, "Failed to resolve ObjectName.");
		return objectName;
	}

	/**
	 * First checks for the presence of a {@link JmxHeaders#OPERATION_NAME} header,
	 * then falls back to this handler's {@link #defaultOperationName} if available.
	 */
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
		else if (message.getPayload() != null && message.getPayload().getClass().isArray()) {
			map = this.createParameterMapFromList(
					Arrays.asList(ObjectUtils.toObjectArray(message.getPayload())));
		}
		else if (message.getPayload() != null) {
			map = this.createParameterMapFromList(Collections.singletonList(message.getPayload()));
		}
		else {
			map = Collections.EMPTY_MAP;
		}
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
