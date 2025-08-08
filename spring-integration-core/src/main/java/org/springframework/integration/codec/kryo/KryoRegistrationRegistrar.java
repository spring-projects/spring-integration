/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec.kryo;

import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Registration;

/**
 * A {@link KryoRegistrar} implementation backed by a List of {@link Registration}.
 *
 * @author David Turanski
 * @since 4.2
 */
public class KryoRegistrationRegistrar extends AbstractKryoRegistrar {

	private final List<Registration> registrations;

	public KryoRegistrationRegistrar(List<Registration> registrations) {
		this.registrations = registrations != null
				? new ArrayList<Registration>(registrations)
				: new ArrayList<Registration>();
	}

	@Override
	public List<Registration> getRegistrations() {
		return this.registrations;
	}

}
