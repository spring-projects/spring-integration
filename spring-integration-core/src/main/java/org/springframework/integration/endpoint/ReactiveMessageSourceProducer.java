/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import java.time.Duration;

import reactor.core.publisher.Flux;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.util.IntegrationReactiveUtils;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * The {@link MessageProducerSupport} to adapt a provided {@link MessageSource}
 * into a {@link Flux} and let it be subscribed in the {@link #subscribeToPublisher}.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class ReactiveMessageSourceProducer extends MessageProducerSupport {

	private final Flux<? extends Message<?>> messageFlux;

	private Duration delayWhenEmpty = IntegrationReactiveUtils.DEFAULT_DELAY_WHEN_EMPTY;

	/**
	 * Create an instance based on the provided {@link MessageSource}.
	 * @param messageSource the {@link MessageSource} to pull for messages.
	 */
	public ReactiveMessageSourceProducer(MessageSource<?> messageSource) {
		Assert.notNull(messageSource, "'messageSource' must not be null");
		this.messageFlux =
				IntegrationReactiveUtils.messageSourceToFlux(messageSource)
						.contextWrite((ctx) ->
								ctx.put(IntegrationReactiveUtils.DELAY_WHEN_EMPTY_KEY, this.delayWhenEmpty));
	}

	/**
	 * Configure a {@link Duration} to delay next pull request when the previous one
	 * was empty. Defaults to {@link IntegrationReactiveUtils#DEFAULT_DELAY_WHEN_EMPTY}.
	 * @param delayWhenEmpty the {@link Duration} to use.
	 */
	public void setDelayWhenEmpty(Duration delayWhenEmpty) {
		Assert.notNull(delayWhenEmpty, "'delayWhenEmpty' must not be null");
		this.delayWhenEmpty = delayWhenEmpty;
	}

	@Override
	protected void doStart() {
		subscribeToPublisher(this.messageFlux);
	}

}
