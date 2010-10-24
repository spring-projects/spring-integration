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
package org.springframework.integration.twitter.inbound;

import org.springframework.integration.twitter.core.DirectMessage;
import org.springframework.integration.twitter.core.twitter.Twitter4jDirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * This class handles support for receiving DMs (direct messages) using Twitter.
 *
 * @author Josh Long
 */
public class InboundDirectMessageEndpoint extends AbstractInboundTwitterEndpointSupport<DirectMessage> {
	
	private Comparator<DirectMessage> dmComparator = new Comparator<DirectMessage>() {
		public int compare(DirectMessage directMessage, DirectMessage directMessage1) {
			return directMessage.getCreatedAt().compareTo(directMessage1.getCreatedAt());
		}
	};

	@Override
	protected void markLastStatusId(DirectMessage dm) {
		this.markerId = dm.getId();
	}

	@Override
	protected List<DirectMessage> sort(List<DirectMessage> rl) {

		List<DirectMessage> dms = new ArrayList<DirectMessage>();

		dms.addAll(rl);

		Collections.sort(dms, dmComparator);

		return dms;
	}

	@Override
	public String getComponentType() {
		return null;  
	}

	@Override
	protected void beginPolling() throws Exception {

		this.runAsAPIRateLimitsPermit(new ApiCallback<InboundDirectMessageEndpoint>() {
			public void run(InboundDirectMessageEndpoint t, Twitter twitter)
					throws Exception {

				List<twitter4j.DirectMessage> dms = !hasMarkedStatus() ? t.twitter.getDirectMessages() : t.twitter.getDirectMessages(new Paging(t.getMarkerId()));

				List<DirectMessage> dmsToFwd = new ArrayList<DirectMessage>();

				for( twitter4j.DirectMessage dm : dms)
					dmsToFwd.add( new Twitter4jDirectMessage( dm));

				forwardAll( dmsToFwd );
			}
		});
	}

}
