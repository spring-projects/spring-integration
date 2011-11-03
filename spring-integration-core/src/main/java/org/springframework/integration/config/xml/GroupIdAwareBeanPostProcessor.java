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

package org.springframework.integration.config.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.store.GroupIdAware;

/**
 * A {@link BeanPostProcessor} implementation that provides default 'groupId' setup strategy for
 * {@link MessageHandler) implementations
 * that also implement {@link GroupIdAware).
 * This postProcessor uses the 'beanName' as 'groupId'.
 *
 * @author Artem Bilan
 * @since 2.1
 */
public class GroupIdAwareBeanPostProcessor implements BeanPostProcessor {

	private final Log logger = LogFactory.getLog(this.getClass());

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof GroupIdAware && bean instanceof MessageHandler) {
			if (logger.isDebugEnabled()) {
				logger.debug("Applying 'groupId' on MessageHandler '" + beanName + "'");
			}
			setupGroupId((GroupIdAware) bean, beanName);
		}
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	private void setupGroupId(GroupIdAware bean, String beanName) {
		if (bean.getGroupId() == null) bean.setGroupId(beanName);
	}
}
