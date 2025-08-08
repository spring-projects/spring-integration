/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.inbound;

import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Polling message source that produces messages from the result of the provided:
 *
 * <ul>
 *     <li>entityClass</li>
 *     <li>JpQl Select Query</li>
 *     <li>Sql Native Query</li>
 *     <li>JpQl Named Query</li>
 *     <li>Sql Native Named Query</li>
 * </ul>.
 * After the objects have been polled, it also possibly to either:
 *
 * <ul>
 *     <li>executes an update (per retrieved object or for the entire payload)</li>
 *     <li>delete the retrieved object</li>
 * </ul>
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.2
 *
 */
public class JpaPollingChannelAdapter extends AbstractMessageSource<Object> {

	private final JpaExecutor jpaExecutor;

	/**
	 * Constructor taking a {@link JpaExecutor} that provide all required JPA
	 * functionality.
	 *
	 * @param jpaExecutor Must not be null.
	 */
	public JpaPollingChannelAdapter(JpaExecutor jpaExecutor) {
		Assert.notNull(jpaExecutor, "jpaExecutor must not be null.");
		this.jpaExecutor = jpaExecutor;
	}

	/**
	 * Check for mandatory attributes.
	 */
	@Override
	protected void onInit() {
		this.jpaExecutor.setBeanFactory(getBeanFactory());
	}

	/**
	 * Use {@link JpaExecutor#poll()} to executes the JPA operation.
	 * Return {@code null} if result of {@link JpaExecutor#poll()} is {@link ObjectUtils#isEmpty}.
	 * The empty collection means there is no data to retrieve from DB at the moment therefore
	 * no reason to emit an empty message from this message source.
	 */
	@Override
	protected Object doReceive() {
		Object result = this.jpaExecutor.poll();
		return ObjectUtils.isEmpty(result) ? null : result;
	}

	@Override
	public String getComponentType() {
		return "jpa:inbound-channel-adapter";
	}

}
