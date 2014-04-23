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

package org.springframework.integration.store;

import java.util.Collection;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;

/**
 * @author Artem Bilan
 * @since 4.0
 */
public class LazyLoadMessagesInterceptor implements MethodInterceptor {

	private final static Log logger = LogFactory.getLog(LazyLoadMessagesInterceptor.class);

	private final MessageGroupStore messageGroupStore;

	private final SimpleMessageGroup messageGroup;

	private final Object groupId;

	private volatile Message<?> oneMessage;

	private LazyLoadMessagesInterceptor(MessageGroupStore messageGroupStore, SimpleMessageGroup messageGroup) {
		this.messageGroupStore = messageGroupStore;
		this.messageGroup = messageGroup;
		this.groupId = messageGroup.getGroupId();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object result = invocation.proceed();
		String methodName = invocation.getMethod().getName();
		if ("getMessages".equals(methodName)) {
			Collection<Message<?>> messages = (Collection<Message<?>>) result;
			if (CollectionUtils.isEmpty(messages)) {
				logger.debug("Lazy loading of messages for messageGroup: " + this.groupId);
				MessageGroup messageGroup = this.messageGroupStore.getMessageGroup(this.groupId);
				messages = messageGroup.getMessages();
				for (Message<?> message : messages) {
					this.messageGroup.add(message);
				}
				this.oneMessage = null;
				return messages;
			}
		}
		else if ("getOne".equals(methodName)) {
			if (result == null) {
				if (this.oneMessage == null) {
					logger.debug("Lazy loading of one message for messageGroup: " + this.groupId);
					this.oneMessage = this.messageGroupStore.getOneMessageFromGroup(this.groupId);
				}
				return this.oneMessage;
			}
		}
		else if ("getSequenceSize".equals(methodName)) {
			if (0 == (Integer) result) {
				if (this.oneMessage == null) {
					logger.debug("Lazy loading of one message for messageGroup: " + this.groupId);
					this.oneMessage = this.messageGroupStore.getOneMessageFromGroup(this.groupId);
				}
				if (this.oneMessage != null) {
					return new IntegrationMessageHeaderAccessor(this.oneMessage).getSequenceSize();
				}
			}
		}
		else {
			if (0 == (Integer) result) {
				return this.messageGroupStore.messageGroupSize(this.groupId);
			}
		}
		return result;
	}


	public static MessageGroup proxyMessageGroup(SimpleMessageGroup messageGroup, MessageGroupStore messageGroupStore,
			ClassLoader classLoader) {
		MethodInterceptor messagesInterceptor = new LazyLoadMessagesInterceptor(messageGroupStore, messageGroup);
		NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(messagesInterceptor);
		advisor.setMappedNames(new String[] {"getMessages", "size", "getOne", "getSequenceSize"});
		ProxyFactory proxyFactory = new ProxyFactory(messageGroup);
		proxyFactory.addAdvisor(advisor);
		return (MessageGroup) proxyFactory.getProxy(classLoader);
	}

}
