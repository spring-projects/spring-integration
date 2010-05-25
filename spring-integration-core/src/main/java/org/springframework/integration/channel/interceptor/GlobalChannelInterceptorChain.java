/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.integration.channel.ChannelInterceptor;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
final class GlobalChannelInterceptorChain implements Ordered{
	private List<ChannelInterceptor> interceptors;
	private String[] patterns;

	private int order;
	
	public GlobalChannelInterceptorChain(List<ChannelInterceptor> interceptors, String[] patterns, int order){
		this.interceptors = interceptors;
		this.patterns = patterns;
		this.order = order;
	}
	
	List<ChannelInterceptor> getInterceptors(){
		return interceptors;
	}
	
	String[] getPatterns() {
		return patterns;
	}

	public String toString(){
		return interceptors.toString();
	}

	public int getOrder() {
		return order;
	}
}
