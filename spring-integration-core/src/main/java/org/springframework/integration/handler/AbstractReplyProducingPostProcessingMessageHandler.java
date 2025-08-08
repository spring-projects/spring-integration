/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
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
	@Nullable
	protected final Object handleRequestMessage(Message<?> requestMessage) {
		Object result = this.doHandleRequestMessage(requestMessage);
		if (this.postProcessWithinAdvice || !this.hasAdviceChain()) {
			this.postProcess(requestMessage, result);
		}
		return result;
	}

	@Override
	@Nullable
	protected final Object doInvokeAdvisedRequestHandler(Message<?> message) {
		Object result = super.doInvokeAdvisedRequestHandler(message);
		if (!this.postProcessWithinAdvice) {
			this.postProcess(message, result);
		}
		return result;
	}

	@Nullable
	protected abstract Object doHandleRequestMessage(Message<?> requestMessage);

}
