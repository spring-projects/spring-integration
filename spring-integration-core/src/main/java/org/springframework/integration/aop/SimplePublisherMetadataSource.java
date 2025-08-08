/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aop;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.expression.Expression;

/**
 * Simple implementation of {@link PublisherMetadataSource} that allows for
 * configuration of a single channel name, payload expression, and
 * array of header key=value expressions.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class SimplePublisherMetadataSource implements PublisherMetadataSource {

	private volatile String channelName;

	private volatile Expression payloadExpression;

	private volatile Map<String, Expression> headerExpressions;

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	@Override
	public String getChannelName(Method method) {
		return this.channelName;
	}

	public void setPayloadExpression(String payloadExpression) {
		this.payloadExpression = EXPRESSION_PARSER.parseExpression(payloadExpression);
	}

	@Override
	public Expression getExpressionForPayload(Method method) {
		return this.payloadExpression;
	}

	public void setHeaderExpressions(Map<String, String> headerExpressions) {
		this.headerExpressions =
				headerExpressions.entrySet()
						.stream()
						.collect(Collectors.toMap(Map.Entry::getKey,
								e -> EXPRESSION_PARSER.parseExpression(e.getValue())));
	}

	@Override
	public Map<String, Expression> getExpressionsForHeaders(Method method) {
		return this.headerExpressions;
	}

}
