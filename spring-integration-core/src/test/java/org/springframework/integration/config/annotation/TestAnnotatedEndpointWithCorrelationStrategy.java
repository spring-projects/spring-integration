/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.annotation;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.CorrelationStrategy;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ReleaseStrategy;

/**
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
@MessageEndpoint("endpointWithCorrelationStrategy")
public class TestAnnotatedEndpointWithCorrelationStrategy {

	@Aggregator(inputChannel = "inputChannel")
	public String aggregatingMethod(List<String> payloads) {
		return payloads.stream()
				.collect(Collectors.joining());
	}

	@ReleaseStrategy
	public boolean isComplete(List<String> payloads) {
		return payloads.size() == 3;
	}

	@CorrelationStrategy
	public String correlate(String payload) {
		return payload.substring(0, 1);
	}

}
