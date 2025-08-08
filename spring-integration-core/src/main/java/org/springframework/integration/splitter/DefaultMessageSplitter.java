/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.springframework.messaging.Message;

/**
 * The default Message Splitter implementation. Returns individual Messages
 * after receiving an array or Collection. If a value is provided for the
 * 'delimiters' property, then String payloads will be tokenized based on
 * those delimiters.
 *
 * @author Mark Fisher
 */
public class DefaultMessageSplitter extends AbstractMessageSplitter {

	private volatile String delimiters;

	/**
	 * Set delimiters to use for tokenizing String values. The default is
	 * <code>null</code> indicating that no tokenization should occur. If
	 * delimiters are provided, they will be applied to any String payload.
	 *
	 * @param delimiters The delimiters.
	 */
	public void setDelimiters(String delimiters) {
		this.delimiters = delimiters;
	}

	@Override
	protected final Object splitMessage(Message<?> message) {
		Object payload = message.getPayload();
		if (payload instanceof String && this.delimiters != null) {
			List<String> tokens = new ArrayList<String>();
			StringTokenizer tokenizer = new StringTokenizer((String) payload, this.delimiters);
			while (tokenizer.hasMoreElements()) {
				tokens.add(tokenizer.nextToken());
			}
			return tokens;
		}
		return payload;
	}

}
