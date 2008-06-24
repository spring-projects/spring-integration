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
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Document;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.transformer.MessageTransformer;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.xml.transform.StringResult;

/**
 * An implementation of {@link MessageTransformer} that delegates to an
 * OXM {@link Marshaller} and/or {@link Unmarshaller}.
 * 
 * @author Mark Fisher
 */
public class XmlPayloadMarshallingTransformer implements MessageTransformer {

	private final Marshaller marshaller;

	private final Unmarshaller unmarshaller;


	public XmlPayloadMarshallingTransformer(Marshaller marshaller, Unmarshaller unmarshaller) {
		this.marshaller = marshaller;
		this.unmarshaller = unmarshaller;
	}

	public XmlPayloadMarshallingTransformer(Marshaller marshaller) {
		this.marshaller = marshaller;
		this.unmarshaller = (marshaller != null && marshaller instanceof Unmarshaller) ?
				(Unmarshaller) marshaller : null;
	}


	@SuppressWarnings("unchecked")
	public void transform(Message message) {
		Object transformedPayload = null;
		Object originalPayload = message.getPayload();
		if (originalPayload instanceof Source) {
			if (this.unmarshaller == null) {
				throw new MessageHandlingException(message, "no Unmarshaller available");
			}
			try {
				transformedPayload = this.unmarshaller.unmarshal((Source) originalPayload);
			}
			catch (IOException e) {
				throw new MessageHandlingException(message, "failed to unmarshal payload", e);
			}
		}
		else {
			if (this.marshaller == null) {
				throw new MessageHandlingException(message, "no Marshaller available");
			}
			Result result = this.createResult(originalPayload);
			if (result == null) {
				throw new MessageHandlingException(message, "Unable to marshal payload; expected ["
					 + String.class.getName() + "] or [" + Document.class.getName() + "] but received ["
					 + originalPayload.getClass().getName() + "].");
			}
			try {
				this.marshaller.marshal(originalPayload, result);
				transformedPayload = result;
			}
			catch (IOException e) {
				throw new MessageHandlingException(message, "failed to marshal payload", e);
			}
		}
		if (transformedPayload == null) {
			throw new MessageHandlingException(message, "failed to transform payload");
		}
		message.setPayload(transformedPayload);
	}

	protected Result createResult(Object objectToMarshal) {
		if (objectToMarshal instanceof String) {
			return new StringResult();
		}
		if (objectToMarshal instanceof Document) {
			return new DOMResult();
		}
		return null;
	}

}
