/*
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
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class KryoClassMapRegistrar extends AbstractKryoRegistrar {

	private final Map<Integer, Class<?>> registeredClasses;

	public KryoClassMapRegistrar(Map<Integer, Class<?>> kryoRegisteredClasses) {
		this.registeredClasses = new HashMap<>(kryoRegisteredClasses);
	}

	@Override
	public List<Registration> getRegistrations() {
		List<Registration> registrations = new ArrayList<>(this.registeredClasses.size());
		if (!CollectionUtils.isEmpty(this.registeredClasses)) {
			for (Map.Entry<Integer, Class<?>> entry : this.registeredClasses.entrySet()) {
				Class<?> type = entry.getValue();
				registrations.add(new Registration(type, KRYO.getSerializer(type), entry.getKey()));
			}
		}
		return registrations;
	}

}
