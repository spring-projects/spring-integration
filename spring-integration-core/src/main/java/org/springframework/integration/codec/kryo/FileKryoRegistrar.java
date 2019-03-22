/*
 * Copyright 2015-2019 the original author or authors.
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

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.esotericsoftware.kryo.Registration;

/**
 * A {@link KryoRegistrar} used to validateRegistration a File serializer.
 *
 * @author David Turanski
 * @author Gary Russell
 * @since 4.2
 */
public class FileKryoRegistrar extends AbstractKryoRegistrar {

	private final int registrationId;

	private final FileSerializer fileSerializer = new FileSerializer();

	public FileKryoRegistrar() {
		this.registrationId = RegistrationIds.DEFAULT_FILE_REGISTRATION_ID;
	}

	/**
	 *
	 * @param registrationId overrides the default registration ID.
	 */
	public FileKryoRegistrar(int registrationId) {
		this.registrationId = registrationId;
	}

	@Override
	public List<Registration> getRegistrations() {
		return Collections.singletonList(new Registration(File.class, this.fileSerializer, this.registrationId));
	}

}
