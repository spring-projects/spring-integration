/*
 * Copyright 2002-2014 the original author or authors.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.util.ClassUtils;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A {@link org.springframework.messaging.MessageHandler} implementation for invoking JMX operations based on
 * the Message sent to its {@link #handleMessage(Message)} method. Message headers
 * will be checked first when resolving the 'objectName' and 'operationName' to be
 * invoked on an MBean. These values would be supplied with the Message headers
 * defined as {@link JmxHeaders#OBJECT_NAME} and {@link JmxHeaders#OPERATION_NAME},
 * respectively. In either case, if no header is present, the value resolution
 * will fallback to the defaults, if any have been configured on this instance via
 * {@link #setObjectName(String)} and {@link #setOperationName(String)},
 * respectively.
 *
 * <p>The operation parameter(s), if any, must be available within the payload of the
 * Message being handled. If the target operation expects multiple parameters, they
 * can be provided in either a List or Map typed payload.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
public class OperationInvokingMessageHandler extends AbstractReplyProducingMessageHandler implements InitializingBean {

	private volatile MBeanServerConnection server;

	private volatile ObjectName objectName;

	private volatile String operationName;

	/**
	 * Provide a reference to the MBeanServer within which the MBean
	 * target for operation invocation has been registered.
	 *
	 * @param server The MBean server connection.
	 */
	public void setServer(MBeanServerConnection server) {
		this.server = server;
	}

	/**
	 * Specify a default ObjectName to use when no such header is
	 * available on the Message being handled.
	 *
	 * @param objectName The object name.
	 */
	public void setObjectName(String objectName) {
		try {
			if (objectName != null) {
				this.objectName = ObjectNameManager.getInstance(objectName);
			}
		}
		catch (MalformedObjectNameException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Specify an operation name to be invoked when no such
	 * header is available on the Message being handled.
	 *
	 * @param operationName The operation name.
	 */
	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	@Override
	public String getComponentType() {
		return "jmx:operation-invoking-channel-adapter";
	}

	@Override
	protected void doInit() {
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
							if (value == null) {
								/*
								 * With Spring 3.2.3 and greater, the parameter names are
								 * registered instead of the JVM's default p1, p2 etc.
								 * Fall back to that naming style if not found.
								 */
								value = paramsFromMessage.get("p" + (index + 1));
							}
							if (value != null && valueTypeMatchesParameterType(value, paramInfo)) {
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
					+ operationName + "' on MBean [" + objectName + "] of type [" + mbeanInfo.getClassName()
					+ "] with " + paramsFromMessage.size() + " parameters: " + paramsFromMessage);
		}
		catch (JMException e) {
			throw new MessageHandlingException(requestMessage, "failed to invoke JMX operation '" +
					operationName + "' on MBean [" + objectName + "]" + " with " +
					paramsFromMessage.size() + " parameters: " + paramsFromMessage, e);
		}
		catch (IOException e) {
			throw new MessageHandlingException(requestMessage, "IOException on MBeanServerConnection", e);
		}
	}

	private boolean valueTypeMatchesParameterType(Object value, MBeanParameterInfo paramInfo) {
		Class<? extends Object> valueClass = value.getClass();
		if (valueClass.getName().equals(paramInfo.getType())) {
			return true;
		}
		else {
			Class<?> primitiveType = ClassUtils.resolvePrimitiveType(valueClass);
			return primitiveType != null && primitiveType.getName().equals(paramInfo.getType());
		}
	}

	/**
	 * First checks if defaultObjectName is set, otherwise falls back on  {@link JmxHeaders#OBJECT_NAME} header.
	 */
	private ObjectName resolveObjectName(Message<?> message) {
		ObjectName objectName = this.objectName;
		if (objectName == null){
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
		}
		Assert.notNull(objectName, "Failed to resolve ObjectName.");
		return objectName;
	}

	/**
	  * First checks if defaultOperationName is set, otherwise falls back on  {@link JmxHeaders#OPERATION_NAME} header.
	 */
	private String resolveOperationName(Message<?> message) {
		String operationName = this.operationName;
		if (operationName == null){
			operationName = message.getHeaders().get(JmxHeaders.OPERATION_NAME, String.class);
		}
		Assert.notNull(operationName, "Failed to resolve operation name.");
		return operationName;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
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

	@SuppressWarnings("rawtypes")
	private Map<String, Object> createParameterMapFromList(List parameters) {
		Map<String, Object> map = new HashMap<String, Object>();
		for (int i = 0; i < parameters.size(); i++) {
			map.put("p" + (i + 1), parameters.get(i));
		}
		return map;
	}

}
