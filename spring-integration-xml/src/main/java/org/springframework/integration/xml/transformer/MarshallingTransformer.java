/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.transformer;

import java.io.IOException;

import javax.xml.transform.Result;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.oxm.Marshaller;
import org.springframework.util.Assert;

/**
 * An implementation of
 * {@link org.springframework.integration.transformer.AbstractTransformer} that delegates
 * to an OXM {@link Marshaller}.
 *
 * @author Mark Fisher
 * @author Jonas Partner
 * @author Gary Russell
 * @author Artem Bilan
 */
public class MarshallingTransformer extends AbstractXmlTransformer {

	private final Marshaller marshaller;

	private final ResultTransformer resultTransformer;

	private volatile boolean extractPayload = true;

	public MarshallingTransformer(Marshaller marshaller, ResultTransformer resultTransformer) {
		Assert.notNull(marshaller, "a marshaller is required");
		this.marshaller = marshaller;
		this.resultTransformer = resultTransformer;
	}

	public MarshallingTransformer(Marshaller marshaller) {
		this(marshaller, null);
	}

	/**
	 * Specify whether the source Message's payload should be extracted prior
	 * to marshalling. This value is set to "true" by default. To send the
	 * Message itself as input to the Marshaller instead, set this to "false".
	 * @param extractPayload true if the payload should be extracted.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	public String getComponentType() {
		return "xml:marshalling-transformer";
	}

	@Override
	public Object doTransform(Message<?> message) {
		Object source = (this.extractPayload) ? message.getPayload() : message;
		Object transformedPayload;
		Result result = getResultFactory().createResult(source);
		if (result == null) {
			throw new MessagingException(
					"Unable to marshal payload, ResultFactory returned null.");
		}
		try {
			this.marshaller.marshal(source, result);
			transformedPayload = result;
		}
		catch (IOException e) {
			throw new MessagingException("Failed to marshal payload", e);
		}
		if (this.resultTransformer != null) {
			transformedPayload = this.resultTransformer.transformResult(result);
		}
		return transformedPayload;
	}

}
