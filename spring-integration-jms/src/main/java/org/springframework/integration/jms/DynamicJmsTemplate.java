/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.jms;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.0.2
 */
public class DynamicJmsTemplate extends JmsTemplate {

	@Override
	public int getPriority() {
		Integer priority = DynamicJmsTemplateProperties.getPriority();
		if (priority == null) {
			return super.getPriority();
		}
		Assert.isTrue(priority >= 0 && priority <= 9, "JMS priority must be in the range of 0-9");
		return priority.intValue();
	}

	@Override
	public long getReceiveTimeout() {
		Long receiveTimeout = DynamicJmsTemplateProperties.getReceiveTimeout();
		return (receiveTimeout != null) ? receiveTimeout.longValue() : super.getReceiveTimeout();
	}

}
