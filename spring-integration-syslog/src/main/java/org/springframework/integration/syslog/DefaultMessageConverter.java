/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.syslog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.transformer.SyslogToMapTransformer;
import org.springframework.messaging.Message;

/**
 * Default {@link MessageConverter}; delegates to a {@link SyslogToMapTransformer}
 * to convert the payload to a map of values and also provides some of the map
 * contents as message headers.
 * See @link {@link SyslogHeaders} for the headers that are mapped.
 * @author Gary Russell
 * @since 3.0
 *
 */
public class DefaultMessageConverter implements MessageConverter, BeanFactoryAware {

	private final SyslogToMapTransformer transformer = new SyslogToMapTransformer();

	public static final Set<String> SYSLOG_PAYLOAD_ENTRIES = new HashSet<String>(
			Arrays.asList(new String[] {SyslogToMapTransformer.MESSAGE, SyslogToMapTransformer.UNDECODED}));

	private volatile BeanFactory beanFactory;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();


	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		return messageBuilderFactory;
	}

	@Override
	public Message<?> fromSyslog(Message<?> message) throws Exception {
		Map<String, ?> map = this.transformer.doTransform(message);
		Map<String, Object> out = new HashMap<String, Object>();
		for (Entry<String, ?> entry : map.entrySet()) {
			String key = entry.getKey();
			if (!SYSLOG_PAYLOAD_ENTRIES.contains(key)) {
				out.put(SyslogHeaders.PREFIX + entry.getKey(), entry.getValue());
			}
		}
		return this.messageBuilderFactory.withPayload(map)
				.copyHeaders(out)
				.build();
	}

}
