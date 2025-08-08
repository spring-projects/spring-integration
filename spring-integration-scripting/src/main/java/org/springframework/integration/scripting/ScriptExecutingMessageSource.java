/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.scripting;

import org.springframework.integration.endpoint.AbstractMessageSource;

/**
 * The {@link org.springframework.integration.core.MessageSource} strategy implementation
 * to produce a {@link org.springframework.messaging.Message} from underlying
 * {@linkplain #scriptMessageProcessor} for polling endpoints.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public class ScriptExecutingMessageSource extends AbstractMessageSource<Object> {

	private final AbstractScriptExecutingMessageProcessor<?> scriptMessageProcessor;

	public ScriptExecutingMessageSource(AbstractScriptExecutingMessageProcessor<?> scriptMessageProcessor) {
		this.scriptMessageProcessor = scriptMessageProcessor;
	}

	@Override
	public String getComponentType() {
		return "inbound-channel-adapter";
	}

	@Override
	protected Object doReceive() {
		return this.scriptMessageProcessor.processMessage(null);
	}

}
