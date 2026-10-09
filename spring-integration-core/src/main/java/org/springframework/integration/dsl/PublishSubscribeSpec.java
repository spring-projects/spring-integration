/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

/**
 * The {@link PublishSubscribeChannelSpec} extension to configure as a general flow callback for sub-flows
 * as subscribers.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class PublishSubscribeSpec extends PublishSubscribeChannelSpec<PublishSubscribeSpec> {

	private final BroadcastPublishSubscribeSpec delegate;

	protected PublishSubscribeSpec() {
		this.delegate = new BroadcastPublishSubscribeSpec(this.channel);
	}

	protected PublishSubscribeSpec(@Nullable Executor executor) {
		super(executor);
		this.delegate = new BroadcastPublishSubscribeSpec(this.channel);
	}

	@Override
	public PublishSubscribeSpec id(String id) {
		return super.id(id);
	}

	public PublishSubscribeSpec subscribe(IntegrationFlow subFlow) {
		this.delegate.subscribe(subFlow);
		return _this();
	}

	@Override
	public Map<Object, @Nullable String> getComponentsToRegister() {
		Map<Object, @Nullable String> objects = new LinkedHashMap<>();
		objects.putAll(super.getComponentsToRegister());
		objects.putAll(this.delegate.getComponentsToRegister());
		return objects;
	}

}
