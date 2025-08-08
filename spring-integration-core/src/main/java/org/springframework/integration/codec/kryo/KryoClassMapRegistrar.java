/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec.kryo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.kryo.Registration;

import org.springframework.util.CollectionUtils;

/**
 * A {@link KryoRegistrar} implementation backed by a Map
 * used to explicitly set the registration ID for each class.
 *
 * @author David Turanski
 * @since 4.2
 */
public class KryoClassMapRegistrar extends AbstractKryoRegistrar {

	private final Map<Integer, Class<?>> registeredClasses;

	public KryoClassMapRegistrar(Map<Integer, Class<?>> kryoRegisteredClasses) {
		this.registeredClasses = new HashMap<Integer, Class<?>>(kryoRegisteredClasses);
	}

	@Override
	public List<Registration> getRegistrations() {
		List<Registration> registrations = new ArrayList<Registration>();
		if (!CollectionUtils.isEmpty(this.registeredClasses)) {
			for (Map.Entry<Integer, Class<?>> entry : this.registeredClasses.entrySet()) {
				registrations.add(
						new Registration(entry.getValue(), KRYO.getSerializer(entry.getValue()), entry.getKey()));
			}
		}
		return registrations;
	}

}
