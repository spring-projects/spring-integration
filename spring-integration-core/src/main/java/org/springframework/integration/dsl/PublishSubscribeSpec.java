/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class PublishSubscribeSpec extends PublishSubscribeChannelSpec<PublishSubscribeSpec> {

	private final Map<Object, String> subscriberFlows = new LinkedHashMap<>();

	PublishSubscribeSpec() {
		super();
	}

	PublishSubscribeSpec(Executor executor) {
		super(executor);
	}

	@Override
	public PublishSubscribeSpec id(String id) {
		return super.id(id);
	}

	public PublishSubscribeSpec subscribe(IntegrationFlow flow) {
		IntegrationFlowBuilder flowBuilder =
				IntegrationFlows.from(this.channel)
						.bridge();
		flow.configure(flowBuilder);
		this.subscriberFlows.put(flowBuilder.get(), null);
		return _this();
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		Map<Object, String> objects = new LinkedHashMap<>();
		objects.putAll(super.getComponentsToRegister());
		objects.putAll(this.subscriberFlows);
		return objects;
	}

}
