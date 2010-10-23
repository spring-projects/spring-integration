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
package org.springframework.integration.twitter;

import twitter4j.Paging;
import twitter4j.Twitter;

import java.util.List;


/**
 * Handles forwarding all new {@link twitter4j.Status} that are 'replies' or 'mentions' to some other tweet.
 *
 * @author Josh Long
 */
public class InboundMentionStatusEndpoint
		extends AbstractInboundTwitterStatusEndpointSupport {


	@Override
	protected void refresh() throws Exception {
		this.runAsAPIRateLimitsPermit(new ApiCallback<InboundMentionStatusEndpoint>() {
			public void run(InboundMentionStatusEndpoint ctx,
							Twitter twitter) throws Exception {
				List<twitter4j.Status> stats = (!hasMarkedStatus())
						? twitter.getMentions()
						: twitter.getMentions(new Paging(ctx.getMarkerId()));


				forwardAll( fromTwitter4jStatus( stats));
			}
		});
	}
}
