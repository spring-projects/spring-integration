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

package org.springframework.integration.endpoint;

import java.util.ArrayList;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;

/**
 * Base class for {@link MessageEndpoint} implementations.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractEndpoint implements MessageEndpoint, BeanNameAware {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile String name;

	private final List<Advice> adviceChain = new ArrayList<Advice>();


	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setBeanName(String beanName) {
		this.setName(beanName);
	}

	public String toString() {
		return (this.name != null) ? this.name : super.toString();
	}

	public void setAdviceChain(List<Object> adviceChain) {
		for (Object advice : adviceChain) {
			if (advice instanceof Advice) {
				this.adviceChain.add((Advice) advice);
			}
			else if (advice instanceof EndpointInterceptor) {
				this.adviceChain.add(new EndpointMethodInterceptor((EndpointInterceptor) advice));
			}
			else {
				throw new ConfigurationException("Each adviceChain element must implement either "
						+ "'" + Advice.class.getName() + "' or '" + EndpointInterceptor.class.getName() + "'.");
			}
		}
	}

	public List<Advice> getAdviceChain() {
		return this.adviceChain;
	}

	public final boolean invoke(Message<?> message) {
		if (message == null) {
			throw new IllegalArgumentException("Message must not be null.");
		}
		if (!this.supports(message)) {
			throw new MessageHandlingException(message, "unsupported message");
		}
		return this.doInvoke(message);
	}

	protected abstract boolean supports(Message<?> message);

	protected abstract boolean doInvoke(Message<?> message);

}
