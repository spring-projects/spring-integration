/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;

/**
 * Represents the metadata associated with a Gateway method. This is most useful when there are
 * multiple methods per Gateway interface.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class GatewayMethodMetadata {

	private final Map<String, Expression> headerExpressions = new HashMap<>();

	private @Nullable Expression payloadExpression;

	private @Nullable String requestChannelName;

	private @Nullable String replyChannelName;

	private @Nullable String requestTimeout;

	private @Nullable String replyTimeout;

	public @Nullable Expression getPayloadExpression() {
		return this.payloadExpression;
	}

	public void setPayloadExpression(@Nullable Expression payloadExpression) {
		this.payloadExpression = payloadExpression;
	}

	public Map<String, Expression> getHeaderExpressions() {
		return this.headerExpressions;
	}

	public void setHeaderExpressions(@Nullable Map<String, Expression> headerExpressions) {
		this.headerExpressions.clear();
		if (headerExpressions != null) {
			this.headerExpressions.putAll(headerExpressions);
		}
	}

	public @Nullable String getRequestChannelName() {
		return this.requestChannelName;
	}

	public void setRequestChannelName(String requestChannelName) {
		this.requestChannelName = requestChannelName;
	}

	public @Nullable String getReplyChannelName() {
		return this.replyChannelName;
	}

	public void setReplyChannelName(String replyChannelName) {
		this.replyChannelName = replyChannelName;
	}

	public @Nullable String getRequestTimeout() {
		return this.requestTimeout;
	}

	public void setRequestTimeout(@Nullable String requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public @Nullable String getReplyTimeout() {
		return this.replyTimeout;
	}

	public void setReplyTimeout(@Nullable String replyTimeout) {
		this.replyTimeout = replyTimeout;
	}

}
