/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.ws.dsl;

import java.util.Arrays;

import org.springframework.integration.ws.SimpleWebServiceOutboundGateway;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.SourceExtractor;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.WebServiceMessageSender;

/**
 * The spec for a {@link SimpleWebServiceOutboundGateway}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.3
 *
 */
public class SimpleWsOutboundGatewaySpec
		extends BaseWsOutboundGatewaySpec<SimpleWsOutboundGatewaySpec, SimpleWebServiceOutboundGateway> {

	protected SourceExtractor<?> sourceExtractor; // NOSONAR

	protected SimpleWsOutboundGatewaySpec(WebServiceTemplate template) {
		this.template = template;
	}

	/**
	 * Configure a {@link SourceExtractor} to use.
	 * @param extractor the extractor.
	 * @return the spec.
	 */
	public SimpleWsOutboundGatewaySpec sourceExtractor(SourceExtractor<?> extractor) {
		this.sourceExtractor = extractor;
		return this;
	}

	/**
	 * Specify a flag to return the whole {@link org.springframework.ws.WebServiceMessage} or build the
	 * {@code payload} based on {@link org.springframework.ws.WebServiceMessage}
	 * and populated headers according {@code headerMapper} configuration.
	 * Defaults to extract payload.
	 * @param extract build payload or return a whole {@link org.springframework.ws.WebServiceMessage}
	 * @return the spec.
	 */
	public SimpleWsOutboundGatewaySpec extractPayload(boolean extract) {
		this.extractPayload = extract;
		return this;
	}


	@Override
	protected SimpleWebServiceOutboundGateway assemble(SimpleWebServiceOutboundGateway gateway) {
		SimpleWebServiceOutboundGateway assembled = super.assemble(gateway);
		assembled.setExtractPayload(this.extractPayload);
		return assembled;
	}

	@Override
	protected SimpleWebServiceOutboundGateway create() {
		SimpleWebServiceOutboundGateway gateway;
		if (this.destinationProvider != null) {
			gateway = new SimpleWebServiceOutboundGateway(this.destinationProvider, this.sourceExtractor,
					this.webServiceMessageFactory);
		}
		else {
			gateway = new SimpleWebServiceOutboundGateway(this.uri, this.sourceExtractor,
					this.webServiceMessageFactory);
		}
		gateway.setWebServiceTemplate(this.template);
		return gateway;
	}

	/**
	 * Spec for a {@link SimpleWebServiceOutboundGateway} where an external
	 * {@link WebServiceTemplate} is not provided.
	 *
	 */
	public static class SimpleWsOutboundGatewayNoTemplateSpec
			extends BaseWsOutboundGatewaySpec<SimpleWsOutboundGatewayNoTemplateSpec, SimpleWebServiceOutboundGateway> {

		protected SourceExtractor<?> sourceExtractor; // NOSONAR

		private boolean extractPayload;

		/**
		 * Configure a {@link SourceExtractor} to use.
		 * @param extractor the extractor.
		 * @return the spec.
		 */
		public SimpleWsOutboundGatewayNoTemplateSpec sourceExtractor(SourceExtractor<?> extractor) {
			this.sourceExtractor = extractor;
			return this;
		}


		/**
		 * Specify the {@link WebServiceMessageFactory} to use.
		 * @param messageFactory the message factory.
		 * @return the spec.
		 */
		public SimpleWsOutboundGatewayNoTemplateSpec messageFactory(WebServiceMessageFactory messageFactory) {
			this.webServiceMessageFactory = messageFactory;
			return this;
		}

		/**
		 * Specify the {@link FaultMessageResolver} to use.
		 * @param resolver the resolver.
		 * @return the spec.
		 */
		public SimpleWsOutboundGatewayNoTemplateSpec faultMessageResolver(FaultMessageResolver resolver) {
			this.faultMessageResolver = resolver;
			return this;
		}

		/**
		 * Specify the {@link WebServiceMessageSender}s to use.
		 * @param senders the senders.
		 * @return the spec.
		 */
		public SimpleWsOutboundGatewayNoTemplateSpec messageSenders(WebServiceMessageSender... senders) {
			this.messageSenders = Arrays.copyOf(senders, senders.length);
			return this;
		}

		/**
		 * Specify the {@link ClientInterceptor}s to use.
		 * @param interceptors the interceptors.
		 * @return the spec.
		 */
		public SimpleWsOutboundGatewayNoTemplateSpec interceptors(ClientInterceptor... interceptors) {
			this.gatewayInterceptors = Arrays.copyOf(interceptors, interceptors.length);
			return this;
		}

		/**
		 * Specify a flag to return the whole {@link org.springframework.ws.WebServiceMessage} or build the
		 * {@code payload} based on {@link org.springframework.ws.WebServiceMessage}
		 * and populated headers according {@code headerMapper} configuration.
		 * Defaults to extract payload.
		 * @param extract build payload or return a whole {@link org.springframework.ws.WebServiceMessage}
		 * @return the spec.
		 */
		public SimpleWsOutboundGatewayNoTemplateSpec extractPayload(boolean extract) {
			this.extractPayload = extract;
			return this;
		}

		@Override
		protected SimpleWebServiceOutboundGateway create() {
			if (this.destinationProvider != null) {
				return new SimpleWebServiceOutboundGateway(this.destinationProvider, this.sourceExtractor,
						this.webServiceMessageFactory);
			}
			else {
				return new SimpleWebServiceOutboundGateway(this.uri, this.sourceExtractor,
						this.webServiceMessageFactory);
			}
		}

		@Override
		protected SimpleWebServiceOutboundGateway assemble(SimpleWebServiceOutboundGateway gateway) {
			SimpleWebServiceOutboundGateway assembled = super.assemble(gateway);
			assembled.setFaultMessageResolver(this.faultMessageResolver);
			assembled.setMessageSenders(this.messageSenders);
			assembled.setInterceptors(this.gatewayInterceptors);
			assembled.setExtractPayload(this.extractPayload);
			return assembled;
		}

	}

}
