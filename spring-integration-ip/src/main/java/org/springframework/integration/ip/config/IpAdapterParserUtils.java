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

package org.springframework.integration.ip.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.ip.tcp.MessageFormats;
import org.springframework.util.StringUtils;

/**
 * Utility methods and constants for IP adapter parsers.
 * 
 * @author Gary Russell
 * @since 2.0
 */
public abstract class IpAdapterParserUtils {

	static final String IP_PROTOCOL_ATTRIBUTE = "protocol";

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
	
	static final String CUSTOM_SOCKET_READER_CLASS_NAME = 
							"custom-socket-reader-class-name";
	
	static final String CUSTOM_SOCKET_WRITER_CLASS_NAME = 
							"custom-socket-writer-class-name";

	static final String SO_LINGER = "so-linger";

	static final String SO_TCP_NODELAY = "so-tcp-no-delay";

	static final String SO_TRAFFIC_CLASS = "so-traffic-class";

	static final String CLOSE = "close";
	
	static final String LOCAL_ADDRESS = "local-address";

	static final String TASK_EXECUTOR = "task-executor";


	/**
	 * Adds a constructor-arg to the provided bean definition builder 
	 * with the value of the attribute whose name is provided if that
	 * attribute is defined in the given element.
	 * 
	 * @param builder the bean definition builder to be configured
	 * @param element the XML element where the attribute should be defined
	 * @param attributeName the name of the attribute whose value will be
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
	 * @param element
	 * @param builder
	 * @param parserContext 
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
	 * Asserts that a protocol attribute (udp or tcp) is supplied,
	 * @param element
	 * @param parserContext 
	 * @return The value of the attribute.
	 * @throws BeanCreationException if attribute not provided or invalid.
	 */
	static String getProtocol(Element element, ParserContext parserContext) {
		String protocol = element.getAttribute(IpAdapterParserUtils.IP_PROTOCOL_ATTRIBUTE);
		if (!StringUtils.hasText(protocol)) {
			parserContext.getReaderContext().error(IpAdapterParserUtils.IP_PROTOCOL_ATTRIBUTE + 
					" is required for an IP channel adapter", element);
		}
		if (!protocol.equals("tcp") && !protocol.equals("udp")) {
			parserContext.getReaderContext().error(IpAdapterParserUtils.IP_PROTOCOL_ATTRIBUTE + 
					" must be 'tcp' or 'udp' for an IP channel adapter", element);
		}
		return protocol;
	}

	/**
	 * Asserts that a port attribute is supplied.
	 * @param element
	 * @param parserContext 
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
	 * @param element
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
	 * Sets the close attribute, if present.
	 * @param element
	 */
	static void setClose(Element element, BeanDefinitionBuilder builder) {
		String close = element.getAttribute(IpAdapterParserUtils.CLOSE);
		if (!StringUtils.hasText(close)) {
			close = "false";
		}
		if (close.equals("true")) {
			builder.addPropertyValue(
					Conventions.attributeNameToPropertyName(IpAdapterParserUtils.CLOSE),
					close);
		}
	}

	/**
	 * Gets the use-nio attribute, if present; if not returns 'false'.
	 * @param element
	 * @return The value of the attribute or false.
	 */
	static String getUseNio(Element element) {
		String useNio = element.getAttribute(IpAdapterParserUtils.USING_NIO);
		if (!StringUtils.hasText(useNio)) {
			useNio = "false";
		}
		return useNio;
	}

	/**
	 * Gets the message-format attribute, if present; if not returns 
	 * {@link MessageFormats#FORMAT_LENGTH_HEADER}.
	 * @param element
	 * @return The value of the attribute or false.
	 */
	static Integer getMessageFormat(Element element) {
		String messageFormat = element.getAttribute(IpAdapterParserUtils.MESSAGE_FORMAT);
		if (!StringUtils.hasText(messageFormat)) {
			return MessageFormats.FORMAT_LENGTH_HEADER;
		}
		if (messageFormat.equals("length-header")) {
			return MessageFormats.FORMAT_LENGTH_HEADER;
		}
		if (messageFormat.equals("stx-etx")) {
			return MessageFormats.FORMAT_STX_ETX;
		}
		if (messageFormat.equals("crlf")) {
			return MessageFormats.FORMAT_CRLF;
		}
		if (messageFormat.equals("serialized")) {
			return MessageFormats.FORMAT_JAVA_SERIALIZED;
		}
		if (messageFormat.equals("custom")) {
			return MessageFormats.FORMAT_CUSTOM;
		}
		return MessageFormats.FORMAT_LENGTH_HEADER;
	}

	/**
	 * @param element
	 * @param builder
	 */
	public static void addOutboundTcpAttributes(Element element,
			BeanDefinitionBuilder builder) {
		builder.addPropertyValue(
				Conventions.attributeNameToPropertyName(IpAdapterParserUtils.MESSAGE_FORMAT), 
				IpAdapterParserUtils.getMessageFormat(element));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, 
				IpAdapterParserUtils.CUSTOM_SOCKET_WRITER_CLASS_NAME); 
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, 
				IpAdapterParserUtils.USING_DIRECT_BUFFERS);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, 
				IpAdapterParserUtils.SO_KEEP_ALIVE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, 
				IpAdapterParserUtils.SO_LINGER);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, 
				IpAdapterParserUtils.SO_TCP_NODELAY);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, 
				IpAdapterParserUtils.SO_TRAFFIC_CLASS);
	}

	/**
	 * Sets the common port attributes on the bean being built (timeout, receive buffer size,
	 * send buffer size).
	 * @param builder 
	 * @param element
	 */
	static void addCommonSocketOptions(BeanDefinitionBuilder builder, Element element) {
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SO_TIMEOUT);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SO_RECEIVE_BUFFER_SIZE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SO_SEND_BUFFER_SIZE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, LOCAL_ADDRESS);
	}

}
