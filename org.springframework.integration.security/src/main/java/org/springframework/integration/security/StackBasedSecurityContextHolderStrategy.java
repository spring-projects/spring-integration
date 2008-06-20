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

package org.springframework.integration.security;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolderStrategy;
import org.springframework.security.context.SecurityContextImpl;
import org.springframework.util.Assert;

/**
 * Covers scenarios where direct channels are used and ensures that an existing
 * {@link SecurityContext} is not unintentionally cleared
 * @author Jonas Partner
 * 
 */
public class StackBasedSecurityContextHolderStrategy implements SecurityContextHolderStrategy {

	private Log logger = LogFactory.getLog(getClass());

	private static ThreadLocal<LinkedList<SecurityContext>> contextHolder = new ThreadLocal<LinkedList<SecurityContext>>();

	public void clearContext() {
		if (getStackForThread().size() > 0) {
			SecurityContext ctx = getStackForThread().removeFirst();
			logger.debug("Popped security context " + ctx);
		}
	}

	public SecurityContext getContext() {
		if (getStackForThread().peek() == null) {
			logger.debug("Pushed new blank security context");
			getStackForThread().addFirst(new SecurityContextImpl());
		}

		return (SecurityContext) getStackForThread().peek();
	}

	public void setContext(SecurityContext context) {
		Assert.notNull(context, "Only non-null SecurityContext instances are permitted");

		getStackForThread().addFirst(context);
		logger.debug("Pushed context " + context);
	}

	protected LinkedList<SecurityContext> getStackForThread() {
		if (contextHolder.get() == null) {
			contextHolder.set(new LinkedList<SecurityContext>());
		}
		return contextHolder.get();
	}

	public void clearStack() {
		if (contextHolder.get() != null) {
			contextHolder.set(null);
		}
	}

}
