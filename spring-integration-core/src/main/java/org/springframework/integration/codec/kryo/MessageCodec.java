/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec.kryo;

/**
 * {@link PojoCodec} configured to encode/decode {@code Message<?>}s.
 *
 * @author Gary Russell
 *
 * @since 4.2
 *
 */
public class MessageCodec extends PojoCodec {

	/**
	 * Construct an instance using the default registration ids for message
	 * headers.
	 */
	public MessageCodec() {
		super(new MessageKryoRegistrar());
	}

	/**
	 * Construct an instance using a custom registrar - usually used if different
	 * registration ids are required for message headers.
	 * @param registrar the registrar.
	 */
	public MessageCodec(MessageKryoRegistrar registrar) {
		super(registrar);
	}

}
