/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.flow.Transaction;

import org.springframework.integration.Message;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author David Turanski
 *
 */
@Transactional
public class TransactionalServiceActivator implements RequestReplyExchanger {
	
	private RequestReplyExchanger gateway;
	public TransactionalServiceActivator(RequestReplyExchanger gateway) {
		this.gateway = gateway;
	}
	/* (non-Javadoc)
	 * @see org.springframework.integration.gateway.RequestReplyExchanger#exchange(org.springframework.integration.Message)
	 */
	public Message<?> exchange(Message<?> request) {
		return gateway.exchange(request);
	}

}
