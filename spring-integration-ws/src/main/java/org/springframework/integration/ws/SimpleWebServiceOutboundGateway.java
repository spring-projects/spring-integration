/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.ws;

import java.io.IOException;
import java.util.Iterator;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.mime.Attachment;
import org.springframework.ws.mime.MimeMessage;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.springframework.xml.transform.TransformerObjectSupport;

/**
 * An outbound Messaging Gateway for invoking a Web Service.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 */
public class SimpleWebServiceOutboundGateway extends AbstractWebServiceOutboundGateway {

	private final SourceExtractor<?> sourceExtractor;

	private boolean extractPayload = true;

	public SimpleWebServiceOutboundGateway(DestinationProvider destinationProvider) {
		this(destinationProvider, null, null);
	}

	public SimpleWebServiceOutboundGateway(DestinationProvider destinationProvider,
			SourceExtractor<?> sourceExtractor) {

		this(destinationProvider, sourceExtractor, null);
	}

	public SimpleWebServiceOutboundGateway(DestinationProvider destinationProvider,
			@Nullable SourceExtractor<?> sourceExtractor,
			WebServiceMessageFactory messageFactory) {

		super(destinationProvider, messageFactory);
		this.sourceExtractor = (sourceExtractor != null) ? sourceExtractor : new DefaultSourceExtractor();
	}

	public SimpleWebServiceOutboundGateway(String uri) {
		this(uri, null, null);
	}

	public SimpleWebServiceOutboundGateway(String uri, SourceExtractor<?> sourceExtractor) {
		this(uri, sourceExtractor, null);
	}

	public SimpleWebServiceOutboundGateway(String uri, @Nullable SourceExtractor<?> sourceExtractor,
			WebServiceMessageFactory messageFactory) {

		super(uri, messageFactory);
		this.sourceExtractor = (sourceExtractor != null) ? sourceExtractor : new DefaultSourceExtractor();
	}

	/**
	 * A flag to return the whole {@link WebServiceMessage} or build
	 * {@code payload} based on {@link WebServiceMessage}
	 * and populated headers according {@code headerMapper} configuration.
	 * Defaults to extract payload.
	 * @param extractPayload build payload or return a whole {@link WebServiceMessage}
	 * @since 5.0
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	public String getComponentType() {
		return "ws:outbound-gateway(simple)";
	}

	@Override
	protected Object doHandle(String uri, final Message<?> requestMessage,
			final WebServiceMessageCallback requestCallback) {

		Object requestPayload = requestMessage.getPayload();
		Result responseResultInstance = null;
		if (requestPayload instanceof String) {
			responseResultInstance = new StringResult();
		}
		else if (requestPayload instanceof Document) {
			responseResultInstance = new DOMResult();
		}
		return getWebServiceTemplate()
				.sendAndReceive(uri,
						new SimpleRequestMessageCallback(requestCallback, requestMessage),
						new SimpleResponseMessageExtractor(responseResultInstance));
	}

	private final class SimpleRequestMessageCallback extends RequestMessageCallback {

		SimpleRequestMessageCallback(WebServiceMessageCallback requestCallback, Message<?> requestMessage) {
			super(requestCallback, requestMessage);
		}

		@Override
		public void doWithMessageInternal(WebServiceMessage message, Object payload)
				throws IOException, TransformerException {
			Source source = this.extractSource(payload);
			transform(source, message.getPayloadResult());
			if (message instanceof MimeMessage && payload instanceof MimeMessage) {
				copyAttachments((MimeMessage) payload, (MimeMessage) message);
			}
		}

		private Source extractSource(Object requestPayload) throws IOException, TransformerException {
			Source source = null;

			if (requestPayload instanceof Source) {
				source = (Source) requestPayload;
				Object o = SimpleWebServiceOutboundGateway.this.sourceExtractor.extractData(source);
				Assert.isInstanceOf(Source.class, o);
				source = (Source) o;
			}
			else if (requestPayload instanceof String) {
				source = new StringSource((String) requestPayload);
			}
			else if (requestPayload instanceof Document) {
				source = new DOMSource((Document) requestPayload);
			}
			else if (requestPayload instanceof WebServiceMessage) {
				source = ((WebServiceMessage) requestPayload).getPayloadSource();
			}
			else {
				throw new MessagingException("Unsupported payload type '" + requestPayload.getClass() +
						"'. " + this.getClass().getName() + " only supports 'java.lang.String', '" +
						Source.class.getName() +
						"', '" + Document.class.getName() + "' and '" + WebServiceMessage.class.getName() + "'. " +
						"Consider either using the '"
						+ MarshallingWebServiceOutboundGateway.class.getName() + "' or a Message Transformer.");
			}

			return source;
		}

		private void copyAttachments(MimeMessage source, MimeMessage target) {
			for (Iterator<Attachment> attachments = source.getAttachments(); attachments.hasNext(); ) {
				Attachment attachment = attachments.next();
				target.addAttachment(attachment.getContentId(), attachment.getDataHandler());
			}
		}

	}

	private final class SimpleResponseMessageExtractor extends ResponseMessageExtractor {

		private final Result result;

		SimpleResponseMessageExtractor(Result result) {
			this.result = result;
		}

		@Override
		public Object doExtractData(WebServiceMessage message) throws TransformerException {
			if (!SimpleWebServiceOutboundGateway.this.extractPayload) {
				return message;
			}
			else {
				Source payloadSource = message.getPayloadSource();

				if (payloadSource != null && this.result != null) {
					this.transform(payloadSource, this.result);
					if (this.result instanceof StringResult) {
						return this.result.toString();
					}
					else if (this.result instanceof DOMResult) {
						return ((DOMResult) this.result).getNode();
					}
					else {
						return this.result;
					}
				}

				return payloadSource;
			}
		}

	}

	private static class DefaultSourceExtractor extends TransformerObjectSupport implements SourceExtractor<DOMSource> {

		DefaultSourceExtractor() {
		}

		@Override
		public DOMSource extractData(Source source) throws TransformerException {
			if (source instanceof DOMSource) {
				return (DOMSource) source;
			}
			DOMResult result = new DOMResult();
			this.transform(source, result);
			return new DOMSource(result.getNode());
		}

	}

}
