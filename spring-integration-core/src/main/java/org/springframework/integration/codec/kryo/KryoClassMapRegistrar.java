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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.esotericsoftware.kryo.Registration;

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
