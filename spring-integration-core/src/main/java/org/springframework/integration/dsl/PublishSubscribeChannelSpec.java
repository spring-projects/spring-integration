/*
 * Copyright 2016-2021 the original author or authors.
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

import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;

/**
 *
 * @param <S> the target {@link PublishSubscribeChannelSpec} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class PublishSubscribeChannelSpec<S extends PublishSubscribeChannelSpec<S>>
		extends MessageChannelSpec<S, PublishSubscribeChannel> {

	protected PublishSubscribeChannelSpec() {
		this(false);
	}

	protected PublishSubscribeChannelSpec(boolean requireSubscribers) {
		this(null, requireSubscribers);
	}

	protected PublishSubscribeChannelSpec(@Nullable Executor executor) {
		this(executor, false);
	}

	protected PublishSubscribeChannelSpec(@Nullable Executor executor, boolean requireSubscribers) {
		this.channel = new PublishSubscribeChannel(executor, requireSubscribers);
	}

	public S errorHandler(ErrorHandler errorHandler) {
		this.channel.setErrorHandler(errorHandler);
		return _this();
	}

	public S ignoreFailures(boolean ignoreFailures) {
		this.channel.setIgnoreFailures(ignoreFailures);
		return _this();
	}

	public S applySequence(boolean applySequence) {
		this.channel.setApplySequence(applySequence);
		return _this();
	}

	public S maxSubscribers(Integer maxSubscribers) {
		this.channel.setMaxSubscribers(maxSubscribers);
		return _this();
	}

	public S minSubscribers(int minSubscribers) {
		this.channel.setMinSubscribers(minSubscribers);
		return _this();
	}

}
