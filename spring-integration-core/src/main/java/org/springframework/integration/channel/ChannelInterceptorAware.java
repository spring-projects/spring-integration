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

	/**
	 * Populate the {@link ChannelInterceptor}s to the target implementation.
	 * @param interceptors the {@link ChannelInterceptor}s to populate.
	 */
	void setInterceptors(List<ChannelInterceptor> interceptors);

	/**
	 * And a {@link ChannelInterceptor} to the target implementation.
	 * @param interceptor the {@link ChannelInterceptor} to add.
	 */
	void addInterceptor(ChannelInterceptor interceptor);

	/**
	 * And a {@link ChannelInterceptor} to the target implementation for the specific index.
	 * @param index the index for {@link ChannelInterceptor} to add.
	 * @param interceptor the {@link ChannelInterceptor} to add.
	 */
	void addInterceptor(int index, ChannelInterceptor interceptor);

	/**
	 * return the {@link ChannelInterceptor} list.
	 * @return the {@link ChannelInterceptor} list.
	 */
	List<ChannelInterceptor> getChannelInterceptors();

	/**
	 * Remove the provided {@link ChannelInterceptor} from the target implementation.
	 * @param interceptor {@link ChannelInterceptor} to remove.
	 * @return the {@code boolean} if {@link ChannelInterceptor} has been removed.
	 */
	boolean removeInterceptor(ChannelInterceptor interceptor);

	/**
	 * Remove a {@link ChannelInterceptor} from the target implementation for specific index.
	 * @param index the index for the {@link org.springframework.messaging.support.ChannelInterceptor} to remove.
	 * @return the {@code boolean} if the {@link ChannelInterceptor} has been removed.
	 */
	ChannelInterceptor removeInterceptor(int index);

}
