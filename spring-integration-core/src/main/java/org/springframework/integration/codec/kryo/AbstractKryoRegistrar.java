/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for {@link KryoRegistrar} implementations.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 4.2
 */
public abstract class AbstractKryoRegistrar implements KryoRegistrar {

	protected static final Kryo KRYO = new Kryo();

	static {
		KRYO.setRegistrationRequired(false);
	}

	protected final Log log = LogFactory.getLog(getClass()); // NOSONAR property is final

	@Override
	public void registerTypes(Kryo kryo) {
		for (Registration registration : getRegistrations()) {
			register(kryo, registration);
		}
	}

	private void register(Kryo kryo, Registration registration) {
		int id = registration.getId();

		Registration existing = kryo.getRegistration(id);

		if (existing != null) {
			throw new IllegalStateException("registration already exists " + existing);
		}

		if (this.log.isInfoEnabled()) {
			this.log.info(String.format("registering %s with serializer %s", registration,
					registration.getSerializer().getClass().getName()));
		}

		kryo.register(registration);
	}

}
