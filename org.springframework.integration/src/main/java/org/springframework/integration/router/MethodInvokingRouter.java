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

package org.springframework.integration.router;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMappingMethodInvoker;

/**
 * A {@link Router} implementation that invokes the specified method
 * on the given object. The method's return value may be a single
 * MessageChannel instance, a single String to be interpreted as
 * a channel name, or a Collection (or Array) of either type.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingRouter extends AbstractRouter implements InitializingBean {

	private final MessageMappingMethodInvoker invoker;


	public MethodInvokingRouter(Object object, Method method) {
		this.invoker = new MessageMappingMethodInvoker(object, method);
	}

	public MethodInvokingRouter(Object object, String methodName) {
		this.invoker = new MessageMappingMethodInvoker(object, methodName);
	}


	public void afterPropertiesSet() throws Exception {
		this.invoker.afterPropertiesSet();
	}

	@Override
	protected Collection<?> resolveChannels(Message<?> message) {
		Object result = this.invoker.invokeMethod(message);
		if (result == null) {
			return null;
		}
		List<Object> channels = new ArrayList<Object>();
		if (result instanceof Collection) {
			channels.addAll((Collection<?>) result);
		}
		else if (result instanceof MessageChannel[]) {
			channels.addAll(Arrays.asList((MessageChannel[]) result));
		}
		else if (result instanceof String[]) {
			channels.addAll(Arrays.asList((String[]) result));
		}
		else if (result instanceof MessageChannel) {
			channels.add((MessageChannel) result);
		}
		else if (result instanceof String) {
			channels.add(result);
		}
		else {
			throw new ConfigurationException(
					"router method must return type 'MessageChannel' or 'String'");
		}
		return channels;
	}

}
