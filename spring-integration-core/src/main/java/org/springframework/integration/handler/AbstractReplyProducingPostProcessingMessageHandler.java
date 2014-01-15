/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public abstract class AbstractReplyProducingPostProcessingMessageHandler
	extends AbstractReplyProducingMessageHandler
	implements PostProcessingMessageHandler {

	private volatile boolean postProcessWithinAdvice = true;

	/**
	 * Specify whether the post processing should occur within
	 * the scope of any configured advice classes. If false, the
	 * post processing will occur after the advice chain returns. Default true.
	 * This is only applicable if there is in fact an advice chain present.
	 *
	 * @param postProcessWithinAdvice true if the post processing should be performed within the advice.
	 */
	public void setPostProcessWithinAdvice(boolean postProcessWithinAdvice) {
		this.postProcessWithinAdvice = postProcessWithinAdvice;
	}

	@Override
	protected final Object handleRequestMessage(Message<?> requestMessage) {
		Object result = this.doHandleRequestMessage(requestMessage);
		if (this.postProcessWithinAdvice || !this.hasAdviceChain()) {
			this.postProcess(requestMessage, result);
		}
		return result;
	}

	@Override
	protected final Object doInvokeAdvisedRequestHandler(Message<?> message) {
		Object result = super.doInvokeAdvisedRequestHandler(message);
		if (!this.postProcessWithinAdvice) {
			this.postProcess(message, result);
		}
		return result;
	}

	protected abstract Object doHandleRequestMessage(Message<?> requestMessage);

}
