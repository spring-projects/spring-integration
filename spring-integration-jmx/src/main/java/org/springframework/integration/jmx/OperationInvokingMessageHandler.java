/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx;

import javax.management.MBeanServerConnection;

import org.springframework.messaging.Message;

/**
 * A {@link org.springframework.messaging.MessageHandler} implementation for invoking JMX operations based on
 * the Message sent to its {@link #handleMessage(Message)} method. Message headers
 * will be checked first when resolving the 'objectName' and 'operationName' to be
 * invoked on an MBean. These values would be supplied with the Message headers
 * defined as {@link JmxHeaders#OBJECT_NAME} and {@link JmxHeaders#OPERATION_NAME},
 * respectively. In either case, if no header is present, the value resolution
 * will fall back to the defaults, if any have been configured on this instance via
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
 *
 * @deprecated since 7.0 in favor {@link org.springframework.integration.jmx.outbound.OperationInvokingMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class OperationInvokingMessageHandler
		extends org.springframework.integration.jmx.outbound.OperationInvokingMessageHandler {

	/**
	 * Construct an instance based on the provided {@link MBeanServerConnection}.
	 * @param server the {@link MBeanServerConnection} to use.
	 * @since 4.3.20
	 */
	public OperationInvokingMessageHandler(MBeanServerConnection server) {
		super(server);
	}

}
