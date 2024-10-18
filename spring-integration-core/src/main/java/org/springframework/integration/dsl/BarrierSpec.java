/*
 * Copyright 2016-2024 the original author or authors.
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

import reactor.util.function.Tuple2;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.BarrierMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandlerSpec} for the {@link BarrierMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class BarrierSpec extends ConsumerEndpointSpec<BarrierSpec, BarrierMessageHandler> {

	private final long timeout;

	private MessageGroupProcessor outputProcessor = new DefaultAggregatingMessageGroupProcessor();

	private CorrelationStrategy correlationStrategy =
			new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID);

	@Nullable
	private MessageChannel discardChannel;

	@Nullable
	private String discardChannelName;

	@Nullable
	private Long triggerTimeout;

	protected BarrierSpec(long timeout) {
		super(null);
		this.timeout = timeout;
	}

	public BarrierSpec outputProcessor(MessageGroupProcessor outputProcessor) {
		Assert.notNull(outputProcessor, "'outputProcessor' must not be null.");
		this.outputProcessor = outputProcessor;
		return this;
	}

	public BarrierSpec correlationStrategy(CorrelationStrategy correlationStrategy) {
		Assert.notNull(correlationStrategy, "'correlationStrategy' must not be null.");
		this.correlationStrategy = correlationStrategy;
		return this;
	}

	/**
	 * Set the channel to which late arriving trigger messages are sent,
	 * or request message does not arrive in time.
	 * @param discardChannel the message channel for discarded triggers.
	 * @return the spec
	 * @since 6.2.10
	 */
	public BarrierSpec discardChannel(@Nullable MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
		return this;
	}

	/**
	 * Set the channel bean name to which late arriving trigger messages are sent,
	 * or request message does not arrive in time.
	 * @param discardChannelName the message channel for discarded triggers.
	 * @return the spec
	 * @since 6.2.10
	 */
	public BarrierSpec discardChannel(@Nullable String discardChannelName) {
		this.discardChannelName = discardChannelName;
		return this;
	}

	/**
	 * Set the timeout in milliseconds when waiting for a request message.
	 * @param triggerTimeout the timeout in milliseconds when waiting for a request message.
	 * @return the spec
	 * @since 6.2.10
	 */
	public BarrierSpec triggerTimeout(long triggerTimeout) {
		this.triggerTimeout = triggerTimeout;
		return this;
	}

	@Override
	public Tuple2<ConsumerEndpointFactoryBean, BarrierMessageHandler> doGet() {
		if (this.triggerTimeout == null) {
			this.handler = new BarrierMessageHandler(this.timeout, this.outputProcessor, this.correlationStrategy);
		}
		else {
			this.handler =
					new BarrierMessageHandler(this.timeout, this.triggerTimeout, this.outputProcessor,
							this.correlationStrategy);
		}
		if (this.discardChannel != null) {
			this.handler.setDiscardChannel(this.discardChannel);
		}
		else if (this.discardChannelName != null) {
			this.handler.setDiscardChannelName(this.discardChannelName);
		}
		return super.doGet();
	}

}
