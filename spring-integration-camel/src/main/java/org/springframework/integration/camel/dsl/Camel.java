/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.camel.dsl;

import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.LambdaRouteBuilder;

import org.springframework.lang.Nullable;

/**
 * Factory class for Apache Camel components DSL.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public final class Camel {

	/**
	 * Create an instance of {@link CamelMessageHandlerSpec} in a {@link ExchangePattern#InOnly} mode.
	 * @return the spec.
	 */
	public static CamelMessageHandlerSpec handler() {
		return camelHandler(null, ExchangePattern.InOnly);
	}

	/**
	 * Create an instance of {@link CamelMessageHandlerSpec} for the provided {@link ProducerTemplate}
	 * in a {@link ExchangePattern#InOnly} mode.
	 * @param producerTemplate the {@link ProducerTemplate} to use.
	 * @return the spec.
	 */
	public static CamelMessageHandlerSpec handler(ProducerTemplate producerTemplate) {
		return camelHandler(producerTemplate, ExchangePattern.InOnly);
	}

	/**
	 * Create an instance of {@link CamelMessageHandlerSpec} in a {@link ExchangePattern#InOut} mode.
	 * @return the spec.
	 */
	public static CamelMessageHandlerSpec gateway() {
		return camelHandler(null, ExchangePattern.InOut);
	}

	/**
	 * Create an instance of {@link CamelMessageHandlerSpec} for the provided {@link ProducerTemplate}
	 * in a {@link ExchangePattern#InOut} mode.
	 * @param producerTemplate the {@link ProducerTemplate} to use.
	 * @return the spec.
	 */
	public static CamelMessageHandlerSpec gateway(ProducerTemplate producerTemplate) {
		return camelHandler(producerTemplate, ExchangePattern.InOut);
	}

	/**
	 * Create an instance of {@link CamelMessageHandlerSpec} for the provided {@link LambdaRouteBuilder}
	 * in a {@link ExchangePattern#InOut} mode.
	 * The {@code CamelContext} is fetched as a bean from the application context.
	 * @param route the {@link LambdaRouteBuilder} to use.
	 * @return the spec.
	 */
	public static CamelMessageHandlerSpec route(LambdaRouteBuilder route) {
		return camelHandler(null, ExchangePattern.InOut)
				.route(route);
	}

	private static CamelMessageHandlerSpec camelHandler(@Nullable ProducerTemplate producerTemplate,
			ExchangePattern exchangePattern) {

		return new CamelMessageHandlerSpec(producerTemplate)
				.exchangePattern(exchangePattern);
	}

	private Camel() {
	}

}
