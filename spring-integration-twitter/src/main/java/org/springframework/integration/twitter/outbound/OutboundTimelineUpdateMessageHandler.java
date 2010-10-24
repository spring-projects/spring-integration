/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.twitter.outbound;

import org.springframework.integration.Message;
import org.springframework.util.Assert;

import twitter4j.StatusUpdate;


/**
 * This class is useful for both sending regular status updates as well as 'replies' or 'mentions'
 *
 * @author Josh Long
 * @since 2.0
 */
public class OutboundTimelineUpdateMessageHandler extends AbstractOutboundTwitterEndpointSupport {
	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		StatusUpdate statusUpdate = this.supportStatusUpdate.fromMessage(message);
		Assert.notNull(statusUpdate, "couldn't send message, unable to build a StatusUpdate instance correctly");
		this.twitter.updateStatus(statusUpdate);
	}

}
