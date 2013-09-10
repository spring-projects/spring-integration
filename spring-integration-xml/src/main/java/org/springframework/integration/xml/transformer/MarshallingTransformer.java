/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xml.transformer;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.integration.xml.result.DomResultFactory;
import org.springframework.integration.xml.result.ResultFactory;
import org.springframework.oxm.Marshaller;
import org.springframework.util.Assert;

/**
 * An implementation of {@link AbstractTransformer} that delegates to an OXM {@link Marshaller}.
 * 
 * @author Mark Fisher
 * @author Jonas Partner
 */
public class MarshallingTransformer extends AbstractTransformer {

	private final Marshaller marshaller;

	private volatile ResultFactory resultFactory;

	private final ResultTransformer resultTransformer;

	private volatile boolean extractPayload = true;


	public MarshallingTransformer(Marshaller marshaller, ResultTransformer resultTransformer) throws ParserConfigurationException {
		Assert.notNull(marshaller, "a marshaller is required");
		this.marshaller = marshaller;
		this.resultTransformer = resultTransformer;
		this.resultFactory = new DomResultFactory();
	}

	public MarshallingTransformer(Marshaller marshaller) throws ParserConfigurationException {
		this(marshaller, null);
	}


	public void setResultFactory(ResultFactory resultFactory) {
		Assert.notNull(resultFactory, "ResultFactory must not be null");
		this.resultFactory = resultFactory;
	}

	/**
	 * Specify whether the source Message's payload should be extracted prior
	 * to marshalling. This value is set to "true" by default. To send the
	 * Message itself as input to the Marshaller instead, set this to "false".
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	public Object doTransform(Message<?> message) {
		Object source = (this.extractPayload) ? message.getPayload() : message;
		Object transformedPayload = null;
		Result result = this.resultFactory.createResult(source);
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
