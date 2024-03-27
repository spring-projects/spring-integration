/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.jmx;

import java.io.IOException;
import java.io.UncheckedIOException;
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

import org.springframework.integration.IntegrationPatternType;
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
 * @author Artem Bilan
 * @author Trung Pham
 *
 * @since 2.0
 */
public class OperationInvokingMessageHandler extends AbstractReplyProducingMessageHandler {

	private final MBeanServerConnection server;

	private ObjectName defaultObjectName;

	private String operationName;

	private boolean expectReply = true;

	/**
	 * Construct an instance based on the provided {@link MBeanServerConnection}.
	 * @param server the {@link MBeanServerConnection} to use.
	 * @since 4.3.20
	 */
	public OperationInvokingMessageHandler(MBeanServerConnection server) {
		Assert.notNull(server, "MBeanServer is required.");
		this.server = server;
	}

	/**
	 * Specify a default ObjectName to use when no such header is
	 * available on the Message being handled.
	 * @param objectName The object name.
	 */
	public void setObjectName(String objectName) {
		try {
			if (objectName != null) {
				this.defaultObjectName = ObjectNameManager.getInstance(objectName);
			}
		}
		catch (MalformedObjectNameException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Specify an operation name to be invoked when no such
	 * header is available on the Message being handled.
	 * @param operationName The operation name.
	 */
	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	/**
	 * Specify whether a reply Message is expected. If not, this handler will simply return null for a
	 * successful response or throw an Exception for a non-successful response. The default is true.
	 * @param expectReply true if a reply is expected.
	 * @since 4.3.20
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	@Override
	public String getComponentType() {
		return this.expectReply ? "jmx:operation-invoking-outbound-gateway" : "jmx:operation-invoking-channel-adapter";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return this.expectReply ? super.getIntegrationPatternType() : IntegrationPatternType.outbound_channel_adapter;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		ObjectName objectName = resolveObjectName(requestMessage);
		String operation = resolveOperationName(requestMessage);
		Map<String, Object> paramsFromMessage = resolveParameters(requestMessage);
		try {
			Object result = invokeOperation(requestMessage, objectName, operation, paramsFromMessage);
			if (!this.expectReply && result != null) {
				if (logger.isWarnEnabled()) {
					logger.warn("This component doesn't expect a reply. " +
							"The MBean operation '" + operation + "' result '" + result +
							"' for '" + objectName + "' is ignored.");
				}
				return null;
			}
			return result;
		}
		catch (JMException ex) {
			throw new MessageHandlingException(requestMessage, "failed to invoke JMX operation '" +
					operation + "' on MBean [" + objectName + "]" + " with " +
					paramsFromMessage.size() + " parameters [" + paramsFromMessage + "] in the [" + this + ']', ex);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private Object invokeOperation(Message<?> requestMessage, ObjectName objectName, String operation,
			Map<String, Object> paramsFromMessage) throws JMException, IOException {

		MBeanInfo mbeanInfo = this.server.getMBeanInfo(objectName);
		MBeanOperationInfo[] opInfoArray = mbeanInfo.getOperations();
		boolean hasNoArgOption = false;
		for (MBeanOperationInfo opInfo : opInfoArray) {
			if (operation.equals(opInfo.getName())) {
				MBeanParameterInfo[] paramInfoArray = opInfo.getSignature();
				if (paramInfoArray.length == 0) {
					hasNoArgOption = true;
				}
				if (paramInfoArray.length == paramsFromMessage.size()) {
					Object[] values = new Object[paramInfoArray.length];
					String[] signature = new String[paramInfoArray.length];
					int index = populateValuesAndSignature(paramsFromMessage, paramInfoArray, values, signature);
					if (index == paramInfoArray.length) {
						return this.server.invoke(objectName, operation, values, signature);
					}
				}
			}
		}
		if (hasNoArgOption) {
			return this.server.invoke(objectName, operation, null, null);
		}
		else {
			throw new MessagingException(requestMessage, "failed to find JMX operation '"
					+ operation + "' on MBean [" + objectName + "] of type [" + mbeanInfo.getClassName()
					+ "] with " + paramsFromMessage.size() + " parameters: " + paramsFromMessage);
		}
	}

	private int populateValuesAndSignature(Map<String, Object> paramsFromMessage, MBeanParameterInfo[] paramInfoArray,
			Object[] values, String[] signature) {

		int index = 0;
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
		return index;
	}

	private boolean valueTypeMatchesParameterType(Object value, MBeanParameterInfo paramInfo) {
		Class<?> valueClass = value.getClass();
		if (valueClass.getName().equals(paramInfo.getType())) {
			return true;
		}
		else {
			Class<?> primitiveType = ClassUtils.resolvePrimitiveType(valueClass);
			return primitiveType != null && primitiveType.getName().equals(paramInfo.getType());
		}
	}

	/**
	 * First checks {@link JmxHeaders#OBJECT_NAME} header in the request message,
	 * otherwise falls back to {@link #defaultObjectName}.
	 */
	private ObjectName resolveObjectName(Message<?> message) {
		ObjectName objectName;
		Object objectNameHeader = message.getHeaders().get(JmxHeaders.OBJECT_NAME);
		if (objectNameHeader != null) {
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
		Assert.notNull(objectName, "Failed to resolve ObjectName: either from headers or 'defaultObjectName'.");
		return objectName;
	}

	/**
	 * First checks if {@link JmxHeaders#OPERATION_NAME} header,
	 * otherwise falls back to {@link #operationName}.
	 */
	private String resolveOperationName(Message<?> message) {
		String operation = message.getHeaders().get(JmxHeaders.OPERATION_NAME, String.class);
		if (operation == null) {
			operation = this.operationName;
		}
		Assert.notNull(operation, "Failed to resolve operation name.");
		return operation;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> resolveParameters(Message<?> message) {
		Map<String, Object> map;
		Object payload = message.getPayload();
		if (payload instanceof Map) {
			map = (Map<String, Object>) payload;
		}
		else if (payload instanceof List) {
			map = createParameterMapFromList((List<?>) payload);
		}
		else if (payload.getClass().isArray()) {
			map = createParameterMapFromList(Arrays.asList(ObjectUtils.toObjectArray(payload)));
		}
		else {
			map = createParameterMapFromList(Collections.singletonList(payload));
		}
		return map;
	}

	private Map<String, Object> createParameterMapFromList(List<?> parameters) {
		Map<String, Object> map = new HashMap<>();
		for (int i = 0; i < parameters.size(); i++) {
			map.put("p" + (i + 1), parameters.get(i));
		}
		return map;
	}

}
