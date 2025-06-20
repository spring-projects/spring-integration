/*
 * Copyright 2016-present the original author or authors.
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

import java.util.concurrent.Executor;

import org.springframework.integration.channel.ExecutorChannel;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class ExecutorChannelSpec extends LoadBalancingChannelSpec<ExecutorChannelSpec, ExecutorChannel> {

	private final Executor executor;

	protected ExecutorChannelSpec(Executor executor) {
		this.executor = executor;
	}

	@Override
	protected ExecutorChannel doGet() {
		this.channel = new ExecutorChannel(this.executor, this.loadBalancingStrategy);
		if (this.failoverStrategy != null) {
			this.channel.setFailoverStrategy(this.failoverStrategy);
		}
		if (this.maxSubscribers != null) {
			this.channel.setMaxSubscribers(this.maxSubscribers);
		}
		return super.doGet();
	}

}
