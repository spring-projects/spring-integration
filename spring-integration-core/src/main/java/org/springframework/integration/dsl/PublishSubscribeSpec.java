/*
 * Copyright 2016-2020 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.lang.Nullable;

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
	public PublishSubscribeSpec id(String id) { // NOSONAR - not useless, increases visibility
		return super.id(id);
	}

	public PublishSubscribeSpec subscribe(IntegrationFlow subFlow) {
		this.delegate.subscribe(subFlow);
		return _this();
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		Map<Object, String> objects = new LinkedHashMap<>();
		objects.putAll(super.getComponentsToRegister());
		objects.putAll(this.delegate.getComponentsToRegister());
		return objects;
	}

}
