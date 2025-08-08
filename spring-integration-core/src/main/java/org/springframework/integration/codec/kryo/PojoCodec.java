/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec.kryo;

import java.util.Collections;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import org.springframework.util.CollectionUtils;

/**
 * Kryo Codec that can encode and decode arbitrary types. Classes and associated
 * {@link com.esotericsoftware.kryo.Serializer}s may be registered via
 * {@link KryoRegistrar}s.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class PojoCodec extends AbstractKryoCodec {

	private final CompositeKryoRegistrar kryoRegistrar;

	private final boolean useReferences;

	public PojoCodec() {
		this.kryoRegistrar = null;
		this.useReferences = true;
	}

	/**
	 * Create an instance with a single KryoRegistrar.
	 * @param kryoRegistrar the registrar.
	 */
	public PojoCodec(KryoRegistrar kryoRegistrar) {
		this(kryoRegistrar != null ? Collections.singletonList(kryoRegistrar) : null, true);
	}

	/**
	 * Create an instance with zero to many KryoRegistrars.
	 * @param kryoRegistrars a list KryoRegistrars.
	 */
	public PojoCodec(List<KryoRegistrar> kryoRegistrars) {
		this.kryoRegistrar = CollectionUtils.isEmpty(kryoRegistrars) ? null :
				new CompositeKryoRegistrar(kryoRegistrars);
		this.useReferences = true;
	}

	/**
	 * Create an instance with a single KryoRegistrar.
	 * @param kryoRegistrar the registrar.
	 * @param useReferences set to false if references are not required (if the object graph is known to be acyclical).
	 * The default is 'true' which is less performant but more flexible.
	 */
	public PojoCodec(KryoRegistrar kryoRegistrar, boolean useReferences) {
		this(kryoRegistrar != null ? Collections.singletonList(kryoRegistrar) : null, useReferences);
	}

	/**
	 * Create an instance with zero to many KryoRegistrars.
	 * @param kryoRegistrars a list KryoRegistrars.
	 * @param useReferences set to false if references are not required (if the object graph is known to be acyclical).
	 * The default is 'true' which is less performant but more flexible.
	 */
	public PojoCodec(List<KryoRegistrar> kryoRegistrars, boolean useReferences) {
		this.kryoRegistrar = CollectionUtils.isEmpty(kryoRegistrars) ? null :
				new CompositeKryoRegistrar(kryoRegistrars);
		this.useReferences = useReferences;
	}

	@Override
	protected void doEncode(Kryo kryo, Object object, Output output) {
		kryo.writeObject(output, object);
	}

	@Override
	protected <T> T doDecode(Kryo kryo, Input input, Class<T> type) {
		return kryo.readObject(input, type);
	}

	@Override
	protected void configureKryoInstance(Kryo kryo) {
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
		if (this.kryoRegistrar != null) {
			this.kryoRegistrar.registerTypes(kryo);
		}
		kryo.setReferences(this.useReferences);
	}

}
