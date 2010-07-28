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

package org.springframework.integration.router;

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.Message;

/**
 * Extends {@link AbstractChannelNameResolvingMessageRouter} to support router
 * implementations that always return a single channel name (or null).
 * 
 * @author Mark Fisher
 */
public abstract class AbstractSingleChannelNameRouter extends AbstractChannelNameResolvingMessageRouter {

	@Override
	protected final List<Object> getChannelIndicatorList(Message<?> message) {
		List<Object> channelList = new ArrayList<Object>();
		String channelName = determineTargetChannelName(message);
		if(channelName != null){
			channelList.add(channelName);
		}
		return channelList;
	}

	/**
	 * Subclasses must implement this method to return the channel name.
	 */
	protected abstract String determineTargetChannelName(Message<?> message);

}
