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

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.twitter.core.TwitterOperations;
import org.springframework.util.Assert;


/**
 * Base adapter class for all outbound Twitter adapters
 *
 * @author Josh Long
 * @since 2.0
 */
public abstract class AbstractOutboundTwitterEndpointSupport extends AbstractMessageHandler {
	protected final TwitterOperations twitter;
	protected final OutboundTweetMessageMapper outboundMaper = new OutboundTweetMessageMapper();

	public AbstractOutboundTwitterEndpointSupport(TwitterOperations twitter){
		Assert.notNull(twitter, "'twitter' must not be null");
		this.twitter = twitter;
	}
}
