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
import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.esotericsoftware.kryo.Registration;

/**
 * A {@link KryoRegistrar} used to validateRegistration a
 * list of Java classes. This assigns a sequential registration ID starting with an initial value (50 by default), but
 * may be configured. This is easiest to set up but requires that every server node be configured with the identical
 * list in the same order.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class KryoClassListRegistrar extends AbstractKryoRegistrar {

	private static final int DEFAULT_INITIAL_ID = 50;

	private final List<Class<?>> registeredClasses;

	private int initialValue = DEFAULT_INITIAL_ID;

	/**
	 * @param classes the vararg of classes to validateRegistration
	 */
	public KryoClassListRegistrar(Class<?>... classes) {
		this(Arrays.asList(classes));
	}

	/**
	 * @param classes the list of classes to validateRegistration
	 */
	public KryoClassListRegistrar(List<Class<?>> classes) {
		this.registeredClasses = new ArrayList<>(classes);
	}

	/**
	 * Set the initial ID value. Classes in the list will be sequentially assigned an ID starting with this value
	 * (default is 50).
	 * @param initialValue the initial value
	 */
	public void setInitialValue(int initialValue) {
		Assert.isTrue(initialValue >= MIN_REGISTRATION_VALUE, "'initialValue' must be >= " + MIN_REGISTRATION_VALUE);
		this.initialValue = initialValue;
	}


	@Override
	public List<Registration> getRegistrations() {
		List<Registration> registrations = new ArrayList<>();
		if (!CollectionUtils.isEmpty(this.registeredClasses)) {
			for (int i = 0; i < this.registeredClasses.size(); i++) {
				registrations.add(new Registration(this.registeredClasses.get(i),
						KRYO.getSerializer(this.registeredClasses.get(i)), i + this.initialValue));
			}
		}
		return registrations;
	}

}
