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
package org.springframework.integration.channel.interceptor;

import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.messaging.support.ChannelInterceptor;

/**
 * {@link ChannelInterceptor}s implementing this interface can veto
 * global interception of a particular channel. Could be used, for example,
 * when an interceptor itself writes to an output channel (which should
 * not be intercepted with this interceptor).
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public interface VetoCapableInterceptor {

	/**
	 * @param beanName The channel name.
	 * @param channel The channel that is about to be intercepted.
	 * @return false if the intercept wishes to veto the interception.
	 */
	boolean shouldIntercept(String beanName, ChannelInterceptorAware channel);

}
