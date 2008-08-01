/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.channel;

import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.SubscribableSource;
import org.springframework.util.Assert;

/**
 * Channel Adapter implementation for a {@link SubscribableSource}.
 * 
 * @author Mark Fisher
 */
public class SubscribableChannelAdapter extends AbstractChannelAdapter implements SubscribableSource {

	private final SubscribableSource source;


	public SubscribableChannelAdapter(String name, SubscribableSource source, MessageTarget target) {
		super(name, target);
		Assert.notNull(source, "source must not be null");
		this.source = source;
	}


	public boolean subscribe(MessageTarget target) {
		return this.source.subscribe(target);
	}

	public boolean unsubscribe(MessageTarget target) {
		return this.source.unsubscribe(target);
	}

}
