/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
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
