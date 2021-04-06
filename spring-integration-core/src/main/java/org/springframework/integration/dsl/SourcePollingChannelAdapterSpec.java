/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.dsl;

import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.scheduling.PollerMetadata;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class SourcePollingChannelAdapterSpec extends
		EndpointSpec<SourcePollingChannelAdapterSpec, SourcePollingChannelAdapterFactoryBean, MessageSource<?>> {

	protected SourcePollingChannelAdapterSpec(MessageSource<?> messageSource) {
		super(messageSource, new SourcePollingChannelAdapterFactoryBean());
		this.endpointFactoryBean.setSource(messageSource);
	}

	public SourcePollingChannelAdapterSpec phase(int phase) {
		this.endpointFactoryBean.setPhase(phase);
		return _this();
	}

	public SourcePollingChannelAdapterSpec autoStartup(boolean autoStartup) {
		this.endpointFactoryBean.setAutoStartup(autoStartup);
		return _this();
	}

	public SourcePollingChannelAdapterSpec poller(PollerMetadata pollerMetadata) {
		if (pollerMetadata != null) {
			if (PollerMetadata.MAX_MESSAGES_UNBOUNDED == pollerMetadata.getMaxMessagesPerPoll()) {
				pollerMetadata.setMaxMessagesPerPoll(1);
			}
			this.endpointFactoryBean.setPollerMetadata(pollerMetadata);
		}
		return _this();
	}

	@Override
	public SourcePollingChannelAdapterSpec role(String role) {
		this.endpointFactoryBean.setRole(role);
		return this;
	}

}
