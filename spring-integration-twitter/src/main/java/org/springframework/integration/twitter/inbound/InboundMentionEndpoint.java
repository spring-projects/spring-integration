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
package org.springframework.integration.twitter.inbound;

import java.util.List;

import org.springframework.integration.MessagingException;

import twitter4j.Paging;

/**
 * Handles forwarding all new {@link twitter4j.Status} that are 'replies' or 'mentions' to some other tweet.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 */
public class InboundMentionEndpoint extends AbstractInboundTwitterStatusEndpointSupport {

	@Override
	public String getComponentType() {
		return  "twitter:inbound-mention-channel-adapter";
	}
	@Override
	Runnable getApiCallback() {
		Runnable apiCallback = new Runnable() {	
			@Override
			public void run() {
				try {
					long sinceId = getMarkerId();
					List<twitter4j.Status> stats = (!hasMarkedStatus())
					? twitter.getMentions()
					: twitter.getMentions(new Paging(sinceId));
					System.out.println("Polling. . . .");
					forwardAll( fromTwitter4jStatuses( stats));
				} catch (Exception e) {
					if (e instanceof RuntimeException){
						throw (RuntimeException)e;
					}
					else {
						throw new MessagingException("Failed to poll for Twitter mentions updates", e);
					}
				}
			}
		};
		return apiCallback;
	}

}
