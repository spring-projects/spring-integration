/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.endpoint.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.endpoint.EndpointInterceptor;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * An {@link EndpointInterceptor} implementation that provides transactional
 * behavior with a {@link PlatformTransactionManager}.
 * 
 * @author Mark Fisher
 */
public class TransactionInterceptor extends EndpointInterceptorAdapter implements InitializingBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final PlatformTransactionManager transactionManager;

	private volatile TransactionTemplate transactionTemplate;

	private volatile String propagationBehaviorName = "PROPAGATION_REQUIRED";

	private volatile String isolationLevelName = "ISOLATION_DEFAULT";

	private volatile int timeout = -1;

	private volatile boolean readOnly = false;


	public TransactionInterceptor(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}


	public void setPropagationBehaviorName(String propagationBehaviorName) {
		this.propagationBehaviorName = propagationBehaviorName;
	}

	public void setIsolationLevelName(String isolationLevelName) {
		this.isolationLevelName = isolationLevelName;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public void afterPropertiesSet() {
		TransactionTemplate template = new TransactionTemplate(this.transactionManager);
		template.setPropagationBehaviorName(this.propagationBehaviorName);
		template.setIsolationLevelName(this.isolationLevelName);
		template.setTimeout(this.timeout);
		template.setReadOnly(this.readOnly);
		this.transactionTemplate = template;
	}

	@Override
	public boolean aroundSend(final Message<?> message, final MessageTarget endpoint) {
		if (this.transactionTemplate == null) {
			throw new ConfigurationException("TransactionInterceptor has not been initialized");
		}
		this.transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				if (logger.isDebugEnabled()) {
					logger.debug("Executing endpoint '" + endpoint + "' within transaction [" + status + "]");
				}
				endpoint.send(message);
				return null;
            }
		});
		return true;
	}

}
