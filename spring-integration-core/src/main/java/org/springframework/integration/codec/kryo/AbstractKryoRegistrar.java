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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;

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
