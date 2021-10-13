/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.xml.transformer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.springframework.integration.transformer.AbstractPayloadTransformer;
import org.springframework.integration.xml.source.DomSourceFactory;
import org.springframework.integration.xml.source.SourceFactory;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.xml.transform.StringSource;

/**
 * An implementation of {@link org.springframework.integration.transformer.Transformer}
 * that delegates to an OXM {@link Unmarshaller}.
 * Expects the payload to be of type {@link Document}, {@link String}, {@link File}, {@link Source}
 * or to have an instance of {@link SourceFactory} that can convert to a {@link Source}.
 * If {@link #alwaysUseSourceFactory} is set to true, then the {@link SourceFactory}
 * will be used to create the {@link Source} regardless of payload type.
 * <p>
 * The {@link #alwaysUseSourceFactory} is ignored if payload is
 * {@link org.springframework.ws.mime.MimeMessage}.
 * <p>
 * The Unmarshaller may return a Message, but if the return value is not
 * already a Message instance, a new Message will be created with that
 * return value as its payload.
 *
 * @author Jonas Partner
 * @author Artem Bilan
 * @author Gary Russell
 */
public class UnmarshallingTransformer extends AbstractPayloadTransformer<Object, Object> {

	private static final boolean MIME_MESSAGE_PRESENT =
			ClassUtils.isPresent("org.springframework.ws.mime.MimeMessage", null);

	private final Unmarshaller unmarshaller;

	private SourceFactory sourceFactory = new DomSourceFactory();

	private boolean alwaysUseSourceFactory = false;

	private MimeMessageUnmarshallerHelper mimeMessageUnmarshallerHelper;

	public UnmarshallingTransformer(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
		if (MIME_MESSAGE_PRESENT) {
			this.mimeMessageUnmarshallerHelper = new MimeMessageUnmarshallerHelper(unmarshaller);
		}
	}

	/**
	 * Provide the SourceFactory to be used. Must not be null.
	 * @param sourceFactory The source factory.
	 */
	public void setSourceFactory(SourceFactory sourceFactory) {
		Assert.notNull(sourceFactory, "sourceFactory must not be null");
		this.sourceFactory = sourceFactory;
	}

	/**
	 * If true always delegate to the {@link SourceFactory}.
	 * @param alwaysUseSourceFactory true to always use the source factory.
	 */
	public void setAlwaysUseSourceFactory(boolean alwaysUseSourceFactory) {
		this.alwaysUseSourceFactory = alwaysUseSourceFactory;
	}

	@Override
	public String getComponentType() {
		return "xml:unmarshalling-transformer";
	}

	@Override // NOSONAR
	public Object transformPayload(Object payload) {
		Source source;
		InputStream inputStream = null;
		try {
			if (this.mimeMessageUnmarshallerHelper != null) {
				Object result = this.mimeMessageUnmarshallerHelper.maybeUnmarshalMimeMessage(payload);
				if (result != null) {
					return result;
				}
			}

			if (this.alwaysUseSourceFactory) {
				source = this.sourceFactory.createSource(payload);
			}
			else if (payload instanceof String) {
				source = new StringSource((String) payload);
			}
			else if (payload instanceof byte[]) {
				source = new StreamSource(new ByteArrayInputStream((byte[]) payload));
			}
			else if (payload instanceof File) {
				File file = (File) payload;
				inputStream = new FileInputStream(file);
				source = new StreamSource(inputStream, file.toURI().toASCIIString());
			}
			else if (payload instanceof Document) {
				source = new DOMSource((Document) payload);
			}
			else if (payload instanceof Source) {
				source = (Source) payload;
			}
			else {
				source = this.sourceFactory.createSource(payload);
			}
			Assert.state(source != null, () ->
					"failed to transform message, payload not assignable from " + Source.class.getName()
							+ "and no conversion possible");

			return this.unmarshaller.unmarshal(source);
		}
		catch (IOException e) {
			throw new UncheckedIOException("failed to unmarshal payload", e);
		}
		finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				}
				catch (IOException e) {
					// Ignore
				}
			}
		}
	}

	private static class MimeMessageUnmarshallerHelper {

		private final Unmarshaller delegate;

		MimeMessageUnmarshallerHelper(Unmarshaller unmarshaller) {
			this.delegate = unmarshaller;
		}

		Object maybeUnmarshalMimeMessage(Object payload) throws IOException {
			if (payload instanceof org.springframework.ws.mime.MimeMessage) {
				return org.springframework.ws.support.MarshallingUtils.unmarshal(this.delegate,
						(org.springframework.ws.mime.MimeMessage) payload);
			}
			else {
				return null;
			}
		}

	}

}
