/*
 * Copyright 2014 the original author or authors.
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

/**
 * A marker interface providing the ability to configure {@link ChannelInterceptor}s
 * on {@link org.springframework.messaging.MessageChannel} implementations.
 * <p>
 * Typically useful when the target {@link org.springframework.messaging.MessageChannel}
 * is an AOP Proxy.
 * *
 * @author Artem Bilan
 * @since 4.0
 */
public interface ChannelInterceptorAware {

	void setInterceptors(List<ChannelInterceptor> interceptors);

	void addInterceptor(ChannelInterceptor interceptor);

	void addInterceptor(int index, ChannelInterceptor interceptor);

	List<ChannelInterceptor> getChannelInterceptors();

}
