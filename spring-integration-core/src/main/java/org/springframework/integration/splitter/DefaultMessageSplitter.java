/*
 * Copyright 2002-2024 the original author or authors.
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
