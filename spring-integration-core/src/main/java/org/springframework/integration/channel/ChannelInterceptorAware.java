/*
 * Copyright 2014-2019 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.List;

import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;

/**
 * A marker interface providing the ability to configure {@link ChannelInterceptor}s
 * on {@link org.springframework.messaging.MessageChannel} implementations.
 * <p>
 * Typically useful when the target {@link org.springframework.messaging.MessageChannel}
 * is an AOP Proxy.
 * *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 *
 * @deprecated since 5.1.3 in favor of {@link InterceptableChannel}.
 * Will be removed in the next version
 */
@Deprecated
public interface ChannelInterceptorAware extends InterceptableChannel {

	/**
	 * return the {@link ChannelInterceptor} list.
	 * @return the {@link ChannelInterceptor} list.
	 */
	default List<ChannelInterceptor> getChannelInterceptors() {
		return getInterceptors();
	}

}
