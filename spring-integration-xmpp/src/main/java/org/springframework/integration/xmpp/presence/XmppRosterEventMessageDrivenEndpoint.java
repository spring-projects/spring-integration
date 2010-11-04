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
package org.springframework.integration.xmpp.presence;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.util.Assert;


/**
 * Describes an endpoint that is able to login as usual with a {@link org.springframework.integration.xmpp.XmppConnectionFactoryBean} and then emit {@link org.springframework.integration.Message}s when a particular event happens to the logged in users {@link org.jivesoftware.smack.Roster}. We try
 * and generically propagate these events. In practical terms, there are a few events worth being notified of: <ul> <li>the {@link org.jivesoftware.smack.packet.Presence} of a user in the {@link org.jivesoftware.smack.Roster} has changed.</li> <li>the actual makeup of the logged-in user's {@link
 * org.jivesoftware.smack.Roster} has changed: entries added, deleted, etc.</li> </ul>
 *
 * @author Josh Long
 * @since 2.0
 */
public class XmppRosterEventMessageDrivenEndpoint extends AbstractEndpoint {

	private static final Log logger = LogFactory.getLog(XmppRosterEventMessageDrivenEndpoint.class);

	private volatile MessageChannel requestChannel;

	private volatile XMPPConnection xmppConnection;

	private InboundMessageMapper<Presence> messageMapper;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();
	
	private final EventForwardingRosterListener rosterListener = new EventForwardingRosterListener();

	private volatile boolean initialized;

	/**
	 * This will be injected or configured via a <em>xmpp-connection-factory</em> element.
	 *
	 * @param xmppConnection the connection
	 */
	public void setXmppConnection(XMPPConnection xmppConnection) {
		this.xmppConnection = xmppConnection;
	}

	/**
	 * @param requestChannel the channel on which the inbound message should be sent
	 */
	public void setRequestChannel(final MessageChannel requestChannel) {
		this.messagingTemplate.setDefaultChannel(requestChannel);
		this.requestChannel = requestChannel;
	}
	
	public void setMessageMapper(InboundMessageMapper<Presence> messageMapper) {
		this.messageMapper = messageMapper;
	}

	@Override
	protected void doStart() {
		Assert.isTrue(this.initialized, this.getComponentType() + " must be initialized");
		this.xmppConnection.getRoster().addRosterListener(rosterListener);
	}

	@Override
	protected void doStop() {
		this.xmppConnection.getRoster().removeRosterListener(rosterListener);
	}

	@Override
	protected void onInit() throws Exception {
		if (null == this.messageMapper) {
			this.messageMapper = new XmppPresenceMessageMapper();
		}

		this.messagingTemplate.afterPropertiesSet();
		this.initialized = true;
	}

	/**
	 * Called whenever an event happesn related to the {@link org.jivesoftware.smack.Roster}
	 *
	 * @param presence the {@link org.jivesoftware.smack.packet.Presence} object representing the new state (optional)
	 */
	private void forwardRosterEventMessage(Presence presence) {
		try {
			Message<?> msg = this.messageMapper.toMessage(presence);
			messagingTemplate.send(requestChannel, msg);
		}
		catch (Exception e) {
			logger.error("Failed to map packet to message ", e);
		}
	}

	/**
	 * Subscribes to a given {@link org.jivesoftware.smack.Roster}s events and forwards them to components on the bus.
	 */
	class EventForwardingRosterListener implements RosterListener {
		public void entriesAdded(final Collection<String> entries) {
			logger.debug("entries added: " + StringUtils.join(entries.iterator(), ","));
		}

		public void entriesUpdated(final Collection<String> entries) {
			logger.debug("entries updated: " + StringUtils.join(entries.iterator(), ","));
		}

		public void entriesDeleted(final Collection<String> entries) {
			logger.debug("entries deleted: " + StringUtils.join(entries.iterator(), ","));
		}

		public void presenceChanged(final Presence presence) {
			logger.debug("presence changed: " + ToStringBuilder.reflectionToString(presence));
			forwardRosterEventMessage(presence);
		}
	}

}
