/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.integration.flow.interceptor;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;
import org.springframework.integration.flow.FlowConstants;
import org.springframework.integration.flow.config.FlowUtils;
import org.springframework.integration.support.MessageBuilder;

/**
 * A ChannelInterceptor to set the Flow output port header
 * @see FlowUtils
 *
 * @author David Turanski
 * 
 */
public class FlowInterceptor extends ChannelInterceptorAdapter {
	private static Log log = LogFactory.getLog(FlowInterceptor.class);

	private final String portName;

	/**	 
	 * @param portName the value of the message header
	 */
	public FlowInterceptor(String portName) {
		this.portName = portName;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {

		log.debug("flow interceptor " + this.hashCode() + " received a message from port " + portName + " on channel "
				+ channel);
		Map<String, Object> headersToCopy = Collections.singletonMap(FlowConstants.FLOW_OUTPUT_PORT_HEADER, (Object) portName);
		return MessageBuilder.fromMessage(message).copyHeadersIfAbsent(headersToCopy).build();

	}
}
