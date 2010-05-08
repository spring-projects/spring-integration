/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.loanbroker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.integration.core.Message;
import org.springframework.integration.loanbroker.domain.LoanQuote;

/**
 * Will aggregate {@link LoanQuote}s based on the value of the 'RESPONSE_TYPE' message header.
 * The value of 'RESPONSE_TYPE' header is set by the 'gateway' and is based on the type of
 * {@link LoanBrokerGateway} method that was invoked by the client.
 * <p>
 * Will return the best {@link LoanQuote} if 'RESPONSE_TYPE' header value is 'BEST' else it will
 * return all quotes.
 * 
 * @author Oleg Zhurakousky
 */
public class LoanQuoteAggregator {

	/**
	 * @param messages
	 * @return
	 */
	@SuppressWarnings({ "unused", "unchecked" })
	private Object aggregateQuotes(List<Message<LoanQuote>> messages) {
		ArrayList payloads = new ArrayList(messages.size());
		for (Message<?> message : messages) {
			payloads.add(message.getPayload());
		}
		String responceType = (String) messages.get(0).getHeaders().get("RESPONSE_TYPE");
		if (responceType.equals("ALL")) {	
			return payloads;
		}
		else {
			Collections.sort(payloads);
			return payloads.get(0);
		}
	}

}
