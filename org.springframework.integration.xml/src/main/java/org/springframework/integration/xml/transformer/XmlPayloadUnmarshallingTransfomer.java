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

import javax.xml.transform.Source;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.transformer.MessageTransformer;
import org.springframework.integration.xml.source.SourceFactory;
import org.springframework.oxm.Unmarshaller;

/**
 * An implementation of {@link MessageTransformer} that delegates to an OXM
 * {@link Unmarshaller}
 * 
 * @author Jonas Partner
 */
public class XmlPayloadUnmarshallingTransfomer implements MessageTransformer {

	private final Unmarshaller unmarshaller;

	private SourceFactory sourceFactory;

	public XmlPayloadUnmarshallingTransfomer(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	public void setSourceFactory(SourceFactory sourceFactory) {
		this.sourceFactory = sourceFactory;
	}

	@SuppressWarnings("unchecked")
	public void transform(Message message) {
		Source source = null;
		if (Source.class.isAssignableFrom(message.getPayload().getClass())) {
			source = (Source) message.getPayload();
		}
		else if (sourceFactory != null) {
			source = sourceFactory.getSourceForMessage(message);
		}

		if (source == null) {
			throw new MessagingException(message,
					"Could not transform message payload not assignable from javax.xml.transform.Source and no conversion possible");
		}

		Object unmarshalled;
		try {
			unmarshalled = unmarshaller.unmarshal(source);
		}
		catch (IOException ioE) {
			throw new MessagingException(message, "Exception unamrshalling payload", ioE);
		}
		message.setPayload(unmarshalled);
	}

}
