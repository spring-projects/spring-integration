/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.handler;

import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.integration.handler.advice.HandleMessageAdvice;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Base class for MessageHandlers that are capable of producing replies.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author David Liu
 */
public abstract class AbstractReplyProducingMessageHandler extends AbstractMessageProducingHandler
		implements BeanClassLoaderAware {

	private volatile RequestHandler advisedRequestHandler;

	private volatile List<Advice> adviceChain;

	private volatile ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private volatile boolean requiresReply = false;

	/**
	 * Flag whether a reply is required. If true an incoming message MUST result in a reply message being sent.
	 * If false an incoming message MAY result in a reply message being sent. Default is false.
	 * @param requiresReply true if a reply is required.
	 */
	public void setRequiresReply(boolean requiresReply) {
		this.requiresReply = requiresReply;
	}

	protected boolean getRequiresReply() {
		return this.requiresReply;
	}

	public void setAdviceChain(List<Advice> adviceChain) {
		Assert.notNull(adviceChain, "adviceChain cannot be null");
		this.adviceChain = adviceChain;
	}

	protected boolean hasAdviceChain() {
		return this.adviceChain != null && this.adviceChain.size() > 0;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	@Override
	protected final void onInit() throws Exception {
		super.onInit();
		if (!CollectionUtils.isEmpty(this.adviceChain)) {
			ProxyFactory proxyFactory = new ProxyFactory(new AdvisedRequestHandler());
			boolean advised = false;
			for (Advice advice : this.adviceChain) {
				if (!(advice instanceof HandleMessageAdvice)) {
					proxyFactory.addAdvice(advice);
					advised = true;
				}
			}
			if (advised) {
				this.advisedRequestHandler = (RequestHandler) proxyFactory.getProxy(this.beanClassLoader);
			}
		}
		doInit();
	}

	protected void doInit() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void handleMessageInternal(Message<?> message) {
		Object result;
		if (this.advisedRequestHandler == null) {
			result = handleRequestMessage(message);
		}
		else {
			result = doInvokeAdvisedRequestHandler(message);
		}
		if (result != null) {
			sendOutputs(result, message);
		}
		else if (this.requiresReply && !isAsync()) {
			throw new ReplyRequiredException(message, "No reply produced by handler '" +
					getComponentName() + "', and its 'requiresReply' property is set to true.");
		}
		else if (!isAsync() && logger.isDebugEnabled()) {
			logger.debug("handler '" + this + "' produced no reply for request Message: " + message);
		}
	}

	protected Object doInvokeAdvisedRequestHandler(Message<?> message) {
		return this.advisedRequestHandler.handleRequestMessage(message);
	}

	/**
	 * Subclasses must implement this method to handle the request Message. The return
	 * value may be a Message, a MessageBuilder, or any plain Object. The base class
	 * will handle the final creation of a reply Message from any of those starting
	 * points. If the return value is null, the Message flow will end here.
	 * @param requestMessage The request message.
	 * @return The result of handling the message, or {@code null}.
	 */
	protected abstract Object handleRequestMessage(Message<?> requestMessage);


	/**
	 * An implementation of this interface is used to wrap the
	 * {@link AbstractReplyProducingMessageHandler#handleRequestMessage(Message)}
	 * method. Also allows access to the underlying
	 * {@link AbstractReplyProducingMessageHandler} to obtain properties.
	 *
	 * @author Gary Russell
	 * @since 2.2
	 *
	 * @see #getAdvisedHandler()
	 *
	 */
	public interface RequestHandler {

		Object handleRequestMessage(Message<?> requestMessage);

		@Override
		String toString();

		/**
		 * Utility method, intended for use in message handler advice classes to get
		 * information about the advised object. For example:
		 * <p>
		 * {@code ((AbstractReplyProducingMessageHandler.RequestHandler)
		 * invocation.getThis()).getAdvisedHandler().getComponentName()}
		 * @return the outer class instance.
		 *
		 * @since 4.3.2
		 */
		AbstractReplyProducingMessageHandler getAdvisedHandler();

	}

	private class AdvisedRequestHandler implements RequestHandler {

		@Override
		public Object handleRequestMessage(Message<?> requestMessage) {
			return AbstractReplyProducingMessageHandler.this.handleRequestMessage(requestMessage);
		}

		@Override
		public String toString() {
			return AbstractReplyProducingMessageHandler.this.toString();
		}

		@Override
		public AbstractReplyProducingMessageHandler getAdvisedHandler() {
			return AbstractReplyProducingMessageHandler.this;
		}

	}

}
