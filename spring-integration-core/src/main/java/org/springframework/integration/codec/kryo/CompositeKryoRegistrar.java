/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec.kryo;

import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A {@link KryoRegistrar} that delegates and validates registrations across all components.
 *
 * @author David Turanski
 * @since 4.2
 */
public class CompositeKryoRegistrar extends AbstractKryoRegistrar {

	private final List<KryoRegistrar> delegates;

	public CompositeKryoRegistrar(List<KryoRegistrar> delegates) {
		this.delegates = new ArrayList<KryoRegistrar>(delegates);

		if (!CollectionUtils.isEmpty(this.delegates)) {
			validateRegistrations();
		}
	}

	@Override
	public void registerTypes(Kryo kryo) {
		for (KryoRegistrar registrar : this.delegates) {
			registrar.registerTypes(kryo);
		}
	}

	@Override
	public final List<Registration> getRegistrations() {
		List<Registration> registrations = new ArrayList<Registration>();
		for (KryoRegistrar registrar : this.delegates) {
			registrations.addAll(registrar.getRegistrations());
		}
		return registrations;
	}

	private void validateRegistrations() {
		List<Integer> ids = new ArrayList<Integer>();
		List<Class<?>> types = new ArrayList<Class<?>>();

		for (Registration registration : getRegistrations()) {
			Assert.isTrue(registration.getId() >= MIN_REGISTRATION_VALUE,
					"registration ID must be >= " + MIN_REGISTRATION_VALUE);
			if (ids.contains(registration.getId())) {
				throw new IllegalArgumentException(String.format("Duplicate registration ID found: %d",
						registration.getId()));
			}
			ids.add(registration.getId());

			if (types.contains(registration.getType())) {
				throw new IllegalArgumentException(String.format("Duplicate registration found for type: %s",
						registration.getType()));
			}
			types.add(registration.getType());

			if (log.isInfoEnabled()) {
				log.info(String.format("configured Kryo registration %s with serializer %s", registration,
						registration.getSerializer().getClass().getName()));
			}
		}
	}

}
