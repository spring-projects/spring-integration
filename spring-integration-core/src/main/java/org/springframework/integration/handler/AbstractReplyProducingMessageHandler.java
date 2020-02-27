/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.handler;

import java.util.LinkedList;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.handler.advice.HandleMessageAdvice;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

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

	private final List<Advice> adviceChain = new LinkedList<>();

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private boolean requiresReply = false;

	private volatile RequestHandler advisedRequestHandler;

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

	/**
	 * Configure a list of {@link Advice}s to proxy a {@link #handleRequestMessage(Message)} method.
	 * @param adviceChain the list of {@link Advice}s to use.
	 */
	public void setAdviceChain(List<Advice> adviceChain) {
		Assert.notEmpty(adviceChain, "adviceChain cannot be empty");
		synchronized (this.adviceChain) {
			this.adviceChain.clear();
			this.adviceChain.addAll(adviceChain);
			if (isInitialized()) {
				initAdvisedRequestHandlerIfAny();
			}
		}
	}

	protected boolean hasAdviceChain() {
		return this.adviceChain.size() > 0;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		// Most out-of-the-box Spring Integration implementations provide an outbound gateway
		// for particular external protocol. If an implementation doesn't belong to this category,
		// it overrides this method to provide its own specific integration pattern type:
		// service-activator, splitter, aggregator, router etc.
		return IntegrationPatternType.outbound_gateway;
	}

	@Override
	protected final void onInit() {
		super.onInit();
		initAdvisedRequestHandlerIfAny();
		doInit();
	}

	private void initAdvisedRequestHandlerIfAny() {
		if (!this.adviceChain.isEmpty()) {
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
	}

	protected void doInit() {
	}

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

	@Nullable
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
	@Nullable
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

		@Nullable
		Object handleRequestMessage(Message<?> requestMessage);

		/**
		 * Utility method, intended for use in message handler advice classes to get
		 * information about the advised object. For example:
		 * <p>
		 * {@code ((AbstractReplyProducingMessageHandler.RequestHandler)
		 * invocation.getThis()).getAdvisedHandler().getComponentName()}
		 * @return the outer class instance.
		 * @since 4.3.2
		 */
		AbstractReplyProducingMessageHandler getAdvisedHandler();

	}

	private class AdvisedRequestHandler implements RequestHandler {

		AdvisedRequestHandler() {
		}

		@Override
		@Nullable
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
