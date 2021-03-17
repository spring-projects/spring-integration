/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import java.util.Arrays;

import org.springframework.lang.Nullable;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 *
 */
public class TcpConnectionInterceptorFactoryChain {

	private TcpConnectionInterceptorFactory[] interceptorFactories;

	@Nullable
	public TcpConnectionInterceptorFactory[] getInterceptorFactories() {
		return this.interceptorFactories; //NOSONAR
	}

	public void setInterceptors(TcpConnectionInterceptorFactory[] interceptorFactories) {
		this.interceptorFactories = Arrays.copyOf(interceptorFactories, interceptorFactories.length);
	}

	public void setInterceptor(TcpConnectionInterceptorFactory... interceptorFactories) {
		this.interceptorFactories = Arrays.copyOf(interceptorFactories, interceptorFactories.length);
	}

}
