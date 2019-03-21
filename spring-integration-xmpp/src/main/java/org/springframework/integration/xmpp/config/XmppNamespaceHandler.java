/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.xmpp.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * This class parses the schema for XMPP support.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class XmppNamespaceHandler extends NamespaceHandlerSupport {

	public static final String XMPP_CONNECTION_BEAN_NAME = "xmppConnection";

	@Override
	public void init() {
		// connection
		registerBeanDefinitionParser("xmpp-connection", new XmppConnectionParser());

		// send/receive messages
		registerBeanDefinitionParser("inbound-channel-adapter", new ChatMessageInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new ChatMessageOutboundChannelAdapterParser());

		// presence
		registerBeanDefinitionParser("presence-inbound-channel-adapter", new PresenceInboundChannelAdapterParser());
		registerBeanDefinitionParser("presence-outbound-channel-adapter", new PresenceOutboundChannelAdapterParser());

		registerBeanDefinitionParser("header-enricher", new XmppHeaderEnricherParser());
	}

}
