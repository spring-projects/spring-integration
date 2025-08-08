/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec.kryo;

import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;

/**
 * Strategy interface used by {@link PojoCodec} to configure registrations
 * classes consistently across {@link Kryo} instances.
 * By default, user defined types are not registered to Kryo.
 * Registration allows a unique ID (small positive integer is ideal) to represent the type
 * in the byte stream. In a distributed environment, all Kryo instances must maintain a
 * consistent registration configuration in order for serialization to function properly.
 * Registrations can result in better performance in demanding situations,
 * but requires some care to maintain. Use this feature only if you really need it.
 *
 * @author David Turanski
 * @since 4.2
 */
public interface KryoRegistrar {

	int MIN_REGISTRATION_VALUE = 10;

	/**
	 * This method is invoked by the {@link PojoCodec} and
	 * applied to the {@link Kryo} instance whenever a new instance is created.
	 * @param kryo the Kryo instance
	 */
	void registerTypes(Kryo kryo);

	/**
	 *
	 * @return the list of {@link Registration} provided
	 */
	List<Registration> getRegistrations();

}
