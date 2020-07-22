/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.codec.kryo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;

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
