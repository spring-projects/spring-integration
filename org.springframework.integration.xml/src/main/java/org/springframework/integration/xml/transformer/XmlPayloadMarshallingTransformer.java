/*
 * Copyright 2002-2008 the original author or authors.
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

import javax.xml.transform.Result;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.transformer.MessageTransformer;
import org.springframework.integration.xml.result.DomResultFactory;
import org.springframework.integration.xml.result.ResultFactory;
import org.springframework.oxm.Marshaller;
import org.springframework.util.Assert;

/**
 * An implementation of {@link MessageTransformer} that delegates to an OXM
 * {@link Marshaller}
 * 
 * @author Mark Fisher
 * @author Jonas Partner
 */
public class XmlPayloadMarshallingTransformer implements MessageTransformer {

	private final Marshaller marshaller;

	private ResultFactory resultFactory = new DomResultFactory();

	public XmlPayloadMarshallingTransformer(Marshaller marshaller) {
		Assert.notNull(marshaller, " a marshaller is required");
		this.marshaller = marshaller;
	}

	public void setResultFactory(ResultFactory resultFactory) {
		Assert.notNull(resultFactory, " the ResultFactory can not be null");
		this.resultFactory = resultFactory;
	}

	@SuppressWarnings("unchecked")
	public void transform(Message message) {
		Object transformedPayload = null;
		Object originalPayload = message.getPayload();
		Result result = this.resultFactory.getNewResult(message);
		if (result == null) {
			throw new MessageHandlingException(message, "Unable to marshall payload, ResultFactory returned null");
		}
		try {
			this.marshaller.marshal(originalPayload, result);
			transformedPayload = result;
		}
		catch (IOException e) {
			throw new MessageHandlingException(message, "failed to marshal payload", e);
		}

		if (transformedPayload == null) {
			throw new MessageHandlingException(message, "failed to transform payload");
		}
		message.setPayload(transformedPayload);
	}

}
