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

package org.springframework.integration.endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.SimplePoller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * An extension of the {@link SimplePoller} that adds concurrency and
 * transactional capabilities.
 * 
 * @author Mark Fisher
 */
public class DefaultEndpointPoller extends SimplePoller {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile TaskExecutor taskExecutor;

	private volatile PlatformTransactionManager transactionManager;

	private volatile TransactionTemplate transactionTemplate;

	private volatile String propagationBehaviorName = "PROPAGATION_REQUIRED";

	private volatile String isolationLevelName = "ISOLATION_DEFAULT";

	private volatile int transactionTimeout = -1;

	private volatile boolean readOnly = false;


	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setPropagationBehaviorName(String propagationBehaviorName) {
		this.propagationBehaviorName = propagationBehaviorName;
	}

	public void setIsolationLevelName(String isolationLevelName) {
		this.isolationLevelName = isolationLevelName;
	}

	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}

	public void setTransactionReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public void afterPropertiesSet() {
		if (this.transactionManager != null) {
			TransactionTemplate template = new TransactionTemplate(this.transactionManager);
			template.setPropagationBehaviorName(this.propagationBehaviorName);
			template.setIsolationLevelName(this.isolationLevelName);
			template.setTimeout(this.transactionTimeout);
			template.setReadOnly(this.readOnly);
			this.transactionTemplate = template;
		}
	}

	public int poll(final MessageSource<?> source, final MessageTarget target) {
		if (this.taskExecutor != null) {
			this.taskExecutor.execute(new Runnable() {
				public void run() {
					doPoll(source, target);
				}
			});
			return 1;
		}
		return doPoll(source, target);
	}

	private int doPoll(final MessageSource<?> source, final MessageTarget target) {
		if (this.transactionTemplate != null) {
			int result = (Integer) this.transactionTemplate.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					if (logger.isDebugEnabled()) {
						logger.debug("Polling source '" + source + "' within transaction [" + status + "]");
					}
					return DefaultEndpointPoller.super.poll(source, target);
				}
            });
			return result;
		}
		return super.poll(source, target);
	}

}
