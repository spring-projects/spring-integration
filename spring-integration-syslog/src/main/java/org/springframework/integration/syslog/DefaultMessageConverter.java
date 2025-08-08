/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
 * Default {@link MessageConverter}; delegates to a {@link SyslogToMapTransformer} to
 * convert the payload to a map of values and also provides some of the map contents as
 * message headers. See @link {@link SyslogHeaders} for the headers that are mapped.
 *
 * @author Gary Russell
 * @author David Liu
 * @since 3.0
 */
public class DefaultMessageConverter implements MessageConverter, BeanFactoryAware {

	private final SyslogToMapTransformer transformer = new SyslogToMapTransformer();

	public static final Set<String> SYSLOG_PAYLOAD_ENTRIES = new HashSet<String>(
			Arrays.asList(new String[] {SyslogToMapTransformer.MESSAGE, SyslogToMapTransformer.UNDECODED}));

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private volatile boolean messageBuilderFactorySet;

	private volatile boolean asMap = true;

	private BeanFactory beanFactory;

	/**
	 * Set false will leave the payload as the original complete syslog.
	 * @param asMap boolean flag.
	 */
	public void setAsMap(boolean asMap) {
		this.asMap = asMap;
	}

	protected boolean asMap() {
		return this.asMap;
	}

	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (!this.messageBuilderFactorySet) {
			if (this.beanFactory != null) {
				this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
			}
			this.messageBuilderFactorySet = true;
		}
		return this.messageBuilderFactory;
	}

	@Override
	public Message<?> fromSyslog(Message<?> message) {
		Map<String, ?> map = this.transformer.doTransform(message);
		Map<String, Object> out = new HashMap<String, Object>();
		for (Entry<String, ?> entry : map.entrySet()) {
			String key = entry.getKey();
			if (!SYSLOG_PAYLOAD_ENTRIES.contains(key)) {
				out.put(SyslogHeaders.PREFIX + entry.getKey(), entry.getValue());
			}
		}
		return getMessageBuilderFactory().withPayload(this.asMap ? map : message.getPayload())
				.copyHeaders(message.getHeaders())
				.copyHeaders(out)
				.build();
	}

}
