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

package org.springframework.integration.ip.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods and constants for IP adapter parsers.
 *
 * @author Gary Russell
 * @since 2.0
 */
public abstract class IpAdapterParserUtils {

	static final String UDP_MULTICAST = "multicast";

	static final String MULTICAST_ADDRESS = "multicast-address";

	static final String PORT = "port";

	static final String HOST = "host";

	static final String CHECK_LENGTH = "check-length";

	static final String SO_TIMEOUT = "so-timeout";

	static final String SO_RECEIVE_BUFFER_SIZE = "so-receive-buffer-size";

	static final String SO_SEND_BUFFER_SIZE = "so-send-buffer-size";

	static final String SO_KEEP_ALIVE = "so-keep-alive";

	static final String RECEIVE_BUFFER_SIZE = "receive-buffer-size";

	static final String POOL_SIZE = "pool-size";

	static final String ACK = "acknowledge";

	static final String ACK_HOST = "ack-host";

	static final String ACK_PORT = "ack-port";

	static final String ACK_TIMEOUT = "ack-timeout";

	static final String MIN_ACKS_SUCCESS = "min-acks-for-success";

	static final String TIME_TO_LIVE = "time-to-live";

	static final String USING_NIO = "using-nio";

	static final String USING_DIRECT_BUFFERS = "using-direct-buffers";

	static final String MESSAGE_FORMAT = "message-format";

	static final String SO_LINGER = "so-linger";

	static final String SO_TCP_NODELAY = "so-tcp-no-delay";

	static final String SO_TRAFFIC_CLASS = "so-traffic-class";

	static final String LOCAL_ADDRESS = "local-address";

	static final String TASK_EXECUTOR = "task-executor";

	static final String TCP_CONNECTION_TYPE = "type";

	static final String SERIALIZER = "serializer";

	static final String DESERIALIZER = "deserializer";

	static final String SINGLE_USE = "single-use";

	static final String TCP_CONNECTION_FACTORY = "connection-factory";

	public static final String INTERCEPTOR_FACTORY_CHAIN = "interceptor-factory-chain";

	public static final String REQUEST_TIMEOUT = "request-timeout";

	public static final String REMOTE_TIMEOUT = "remote-timeout";

	public static final String REPLY_TIMEOUT = "reply-timeout";

	public static final String REPLY_CHANNEL = "reply-channel";

	public static final String LOOKUP_HOST = "lookup-host";

	public static final String APPLY_SEQUENCE = "apply-sequence";

	public static final String CLIENT_MODE = "client-mode";

	public static final String RETRY_INTERVAL = "retry-interval";

	public static final String SCHEDULER = "scheduler";

	public static final String SSL_CONTEXT_SUPPORT = "ssl-context-support";

	public static final String SOCKET_SUPPORT = "socket-support";

	public static final String SOCKET_FACTORY_SUPPORT = "socket-factory-support";

	public static final String BACKLOG = "backlog";

	public static final String MAPPER = "mapper";

	private IpAdapterParserUtils() {}

	/**
	 * Adds a constructor-arg to the provided bean definition builder
	 * with the value of the attribute whose name is provided if that
	 * attribute is defined in the given element.
	 *
	 * @param builder the bean definition builder to be configured
	 * @param element the XML element where the attribute should be defined
	 * @param attributeName the name of the attribute whose value will be
	 * @param trueFalse not used
	 * used to populate the property
	 */
	public static void addConstuctorValueIfAttributeDefined(BeanDefinitionBuilder builder,
			Element element, String attributeName, boolean trueFalse) {
		String attributeValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(attributeValue)) {
			builder.addConstructorArgValue(attributeValue);
		}
	}

	/**
	 * @param element The element.
	 * @param builder The builder.
	 * @param parserContext The parser context.
	 */
	public static void addHostAndPortToConstructor(Element element,
			BeanDefinitionBuilder builder, ParserContext parserContext) {
		String host = element.getAttribute(IpAdapterParserUtils.HOST);
		if (!StringUtils.hasText(host)) {
			parserContext.getReaderContext().error(IpAdapterParserUtils.HOST
					+ " is required for IP outbound channel adapters", element);
		}
		builder.addConstructorArgValue(host);
		String port = IpAdapterParserUtils.getPort(element, parserContext);
		builder.addConstructorArgValue(port);
	}

	/**
	 * @param element The element.
	 * @param builder The builder.
	 * @param parserContext The parser context.
	 */
	public static void addPortToConstructor(Element element,
			BeanDefinitionBuilder builder, ParserContext parserContext) {
		String port = IpAdapterParserUtils.getPort(element, parserContext);
		builder.addConstructorArgValue(port);
	}

	/**
	 * Asserts that a port attribute is supplied.
	 * @param element The element.
	 * @param parserContext The parser context.
	 * @return The value of the attribute.
	 * @throws BeanCreationException if attribute is not provided.
	 */
	static String getPort(Element element, ParserContext parserContext) {
		String port = element.getAttribute(IpAdapterParserUtils.PORT);
		if (!StringUtils.hasText(port)) {
			parserContext.getReaderContext().error(IpAdapterParserUtils.PORT +
					" is required for IP channel adapters", element);
		}
		return port;
	}

	/**
	 * Gets the multicast attribute, if present; if not returns 'false'.
	 * @param element The element.
	 * @return The value of the attribute or false.
	 */
	static String getMulticast(Element element) {
		String multicast = element.getAttribute(IpAdapterParserUtils.UDP_MULTICAST);
		if (!StringUtils.hasText(multicast)) {
			multicast = "false";
		}
		return multicast;
	}

	/**
	 * Sets the common port attributes on the bean being built (timeout, receive buffer size,
	 * send buffer size).
	 * @param builder The builder.
	 * @param element The element.
	 */
	static void addCommonSocketOptions(BeanDefinitionBuilder builder, Element element) {
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SO_TIMEOUT);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SO_RECEIVE_BUFFER_SIZE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SO_SEND_BUFFER_SIZE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, LOCAL_ADDRESS);
	}

}
