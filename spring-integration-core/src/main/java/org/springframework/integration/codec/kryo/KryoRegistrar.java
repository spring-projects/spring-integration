/*
 * Copyright 2015-2024 the original author or authors.
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
