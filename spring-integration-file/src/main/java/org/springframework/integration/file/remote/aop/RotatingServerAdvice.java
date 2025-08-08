/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.file.remote.aop;

import java.util.List;

import org.springframework.integration.aop.MessageSourceMutator;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A smart poller advice that rotates across multiple remote servers/directories.
 *
 * @author Gary Russell
 * @author Michael Forstner
 * @author Artem Bilan
 * @author David Turanski
 *
 * @since 5.0.7
 *
 */
public class RotatingServerAdvice implements MessageSourceMutator {

	private final RotationPolicy rotationPolicy;

	/**
	 * Create an instance that rotates to the next server/directory if no message is
	 * received.
	 * @param factory the {@link DelegatingSessionFactory}.
	 * @param keyDirectories a list of {@link RotationPolicy.KeyDirectory}.
	 */
	public RotatingServerAdvice(DelegatingSessionFactory<?> factory, List<RotationPolicy.KeyDirectory> keyDirectories) {
		this(factory, keyDirectories, false);
	}

	/**
	 * Create an instance that rotates to the next server/directory depending on the fair
	 * argument.
	 * @param factory the {@link DelegatingSessionFactory}.
	 * @param keyDirectories a list of {@link RotationPolicy.KeyDirectory}.
	 * @param fair true to rotate on every poll, false to rotate when no message is received.
	 */
	public RotatingServerAdvice(DelegatingSessionFactory<?> factory, List<RotationPolicy.KeyDirectory> keyDirectories,
			boolean fair) {

		this(new StandardRotationPolicy(factory, keyDirectories, fair));
	}

	/**
	 * Construct an instance that rotates according to the supplied
	 * {@link RotationPolicy}.
	 * @param rotationPolicy the policy.
	 */
	public RotatingServerAdvice(RotationPolicy rotationPolicy) {
		Assert.notNull(rotationPolicy, "'rotationPolicy' cannot be null");
		this.rotationPolicy = rotationPolicy;
	}

	@Override
	public boolean beforeReceive(MessageSource<?> source) {
		this.rotationPolicy.beforeReceive(source);
		return true;
	}

	@Override
	@Nullable
	public Message<?> afterReceive(@Nullable Message<?> result, MessageSource<?> source) {
		this.rotationPolicy.afterReceive(result != null, source);
		return result;
	}

}
