/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.handler.advice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.util.Assert;

/**
 * RecoveryCallback that sends the final throwable as an ErrorMessage after
 * retry exhaustion.
 * @author Gary Russell
 * @since 2.2
 *
 */
public class ErrorMessageSendingRecoverer implements RecoveryCallback<Object> {

	private final static Log logger = LogFactory.getLog(ErrorMessageSendingRecoverer.class);

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	public ErrorMessageSendingRecoverer(MessageChannel channel) {
		Assert.notNull(channel, "channel cannot be null");
		this.messagingTemplate.setDefaultChannel(channel);
	}

	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	public Object recover(RetryContext context) throws Exception {
		Throwable lastThrowable = context.getLastThrowable();
		if (logger.isDebugEnabled()) {
			String supplement = "";
			if (lastThrowable instanceof MessagingException) {
				supplement = ":failedMessage:" + ((MessagingException) lastThrowable).getFailedMessage();
			}
			logger.debug("Sending ErrorMessage " + supplement, lastThrowable);
		}
		messagingTemplate.send(new ErrorMessage(lastThrowable));
		return null;
	}

}
