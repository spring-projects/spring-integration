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

package org.springframework.integration.xmpp.inbound;

import java.util.Collection;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jxmpp.jid.Jid;

import org.springframework.integration.xmpp.core.AbstractXmppConnectionAwareEndpoint;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An inbound endpoint that is able to login and then emit
 * particular Presence event occurs within the logged-in user's {@link Roster}.
 * (e.g., logged in/out, changed status etc.)
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @author Trung Pham
 *
 * @since 2.0
 */
public class PresenceListeningEndpoint extends AbstractXmppConnectionAwareEndpoint {

	private final PresencePublishingRosterListener rosterListener = new PresencePublishingRosterListener();

	public PresenceListeningEndpoint() {
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
		Assert.isTrue(isInitialized(), () -> getComponentName() + " [" + getComponentType() + "] must be initialized");
		Roster roster = Roster.getInstanceFor(getXmppConnection());
		roster.addRosterListener(this.rosterListener);
	}

	@Override
	protected void doStop() {
		XMPPConnection xmppConnection = getXmppConnection();
		if (xmppConnection != null) {
			Roster.getInstanceFor(xmppConnection).removeRosterListener(this.rosterListener);
		}
	}

	/**
	 * RosterListener that subscribes to a given {@link Roster}'s events.
	 * Presence changes will be published to a message channel.
	 * All other roster events are only logged at debug level.
	 */
	private class PresencePublishingRosterListener implements RosterListener {

		PresencePublishingRosterListener() {
		}

		@Override
		public void entriesAdded(Collection<Jid> entries) {
			logger.debug(() -> "entries added: " + StringUtils.collectionToCommaDelimitedString(entries));
		}

		@Override
		public void entriesUpdated(Collection<Jid> entries) {
			logger.debug(() -> "entries updated: " + StringUtils.collectionToCommaDelimitedString(entries));
		}

		@Override
		public void entriesDeleted(Collection<Jid> entries) {
			logger.debug(() -> "entries deleted: " + StringUtils.collectionToCommaDelimitedString(entries));
		}

		@Override
		public void presenceChanged(Presence presence) {
			if (presence != null) {
				logger.debug(() -> "presence changed: " + presence.getFrom() + " - " + presence);
				sendMessage(PresenceListeningEndpoint.this.getMessageBuilderFactory().withPayload(presence).build());
			}
		}

	}

}
