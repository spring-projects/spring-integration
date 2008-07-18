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

import org.springframework.integration.message.MessagingException;
import org.springframework.integration.transformer.PayloadTransformer;
import org.springframework.integration.xml.source.DomSourceFactory;
import org.springframework.integration.xml.source.SourceFactory;
import org.springframework.oxm.Unmarshaller;

/**
 * An implementation of {@link PayloadTransformer} that delegates to an OXM
 * {@link Unmarshaller}. Expects the payload to be of type {@link Source} or to
 * have an instance of {@link SourceFactory} that can convert to a
 * {@link Source}.
 * 
 * @author Jonas Partner
 */
public class XmlPayloadUnmarshallingTransformer implements PayloadTransformer<Object, Object> {

	private final Unmarshaller unmarshaller;

	private SourceFactory sourceFactory = new DomSourceFactory();


	public XmlPayloadUnmarshallingTransformer(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}


	public void setSourceFactory(SourceFactory sourceFactory) {
		this.sourceFactory = sourceFactory;
	}

	public Object transform(Object payload) {
		Source source = null;
		if (Source.class.isAssignableFrom(payload.getClass())) {
			source = (Source) payload;
		}
		else if (this.sourceFactory != null) {
			source = this.sourceFactory.createSource(payload);
		}

		if (source == null) {
			throw new MessagingException(
					"Failed to transform message, payload not assignable from javax.xml.transform.Source and no conversion possible");
		}

		try {
			return this.unmarshaller.unmarshal(source);
		}
		catch (IOException e) {
			throw new MessagingException("Failed to unamrshal payload", e);
		}
	}

}
