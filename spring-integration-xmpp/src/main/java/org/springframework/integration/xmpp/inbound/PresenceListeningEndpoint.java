/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.integration.xmpp.inbound;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jxmpp.jid.Jid;

import org.springframework.integration.xmpp.core.AbstractXmppConnectionAwareEndpoint;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An inbound endpoint that is able to login and then emit {@link Message}s when a
 * particular Presence event occurs within the logged-in user's {@link Roster}.
 * (e.g., logged in/out, changed status etc.)
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
public class PresenceListeningEndpoint extends AbstractXmppConnectionAwareEndpoint {

	private static final Log logger = LogFactory.getLog(PresenceListeningEndpoint.class);


	private final PresencePublishingRosterListener rosterListener = new PresencePublishingRosterListener();


	public PresenceListeningEndpoint() {
		super();
	}

	public PresenceListeningEndpoint(XMPPConnection xmppConnection) {
		super(xmppConnection);
	}


	@Override
	public String getComponentType() {
		return "xmpp:presence-inbound-channel-adapter";
	}

	@Override
	protected void doStart() {
		Assert.isTrue(this.initialized, this.getComponentName() + " [" + this.getComponentType() + "] must be initialized");
		Roster roster = Roster.getInstanceFor(this.xmppConnection);
		roster.addRosterListener(this.rosterListener);
	}

	@Override
	protected void doStop() {
		if (this.xmppConnection != null) {
			Roster.getInstanceFor(this.xmppConnection).removeRosterListener(this.rosterListener);
		}
	}


	/**
	 * RosterListener that subscribes to a given {@link Roster}'s events.
	 * Presence changes will be published to a message channel.
	 * All other roster events are only logged at debug level.
	 */
	private class PresencePublishingRosterListener implements RosterListener {

		PresencePublishingRosterListener() {
			super();
		}

		@Override
		public void entriesAdded(Collection<Jid> entries) {
			if (logger.isDebugEnabled()) {
				logger.debug("entries added: " + StringUtils.collectionToCommaDelimitedString(entries));
			}
		}

		@Override
		public void entriesUpdated(Collection<Jid> entries) {
			if (logger.isDebugEnabled()) {
				logger.debug("entries updated: " + StringUtils.collectionToCommaDelimitedString(entries));
			}
		}

		@Override
		public void entriesDeleted(Collection<Jid> entries) {
			if (logger.isDebugEnabled()) {
				logger.debug("entries deleted: " + StringUtils.collectionToCommaDelimitedString(entries));
			}
		}

		@Override
		public void presenceChanged(Presence presence) {
			if (presence != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("presence changed: " + presence.getFrom() + " - " + presence);
				}
				sendMessage(PresenceListeningEndpoint.this.getMessageBuilderFactory().withPayload(presence).build());
			}
		}
	}

}
