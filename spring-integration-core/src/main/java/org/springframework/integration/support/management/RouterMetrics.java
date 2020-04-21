/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.support.management;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.springframework.context.Lifecycle;

/**
 * Allows Router operations to appear in the same MBean as statistics.
 *
 * @author Gary Russell
 * @since 4.2
 *
 * @deprecated in favor of Micrometer metrics.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class RouterMetrics extends LifecycleMessageHandlerMetrics implements MappingMessageRouterManagement {

	private final MappingMessageRouterManagement router;

	public RouterMetrics(Lifecycle lifecycle, MappingMessageRouterManagement delegate) {
		super(lifecycle, (MessageHandlerMetrics) delegate);
		this.router = delegate;
	}

	@Override
	public void setChannelMapping(String key, String channelName) {
		this.router.setChannelMapping(key, channelName);
	}

	@Override
	public void removeChannelMapping(String key) {
		this.router.removeChannelMapping(key);
	}

	@Override
	public void replaceChannelMappings(Properties channelMappings) {
		this.router.replaceChannelMappings(channelMappings);
	}

	@Override
	public Map<String, String> getChannelMappings() {
		return this.router.getChannelMappings();
	}

	@Override
	public void setChannelMappings(Map<String, String> channelMappings) {
		this.router.setChannelMappings(channelMappings);
	}

	@Override
	public Collection<String> getDynamicChannelNames() {
		return this.router.getDynamicChannelNames();
	}

}
