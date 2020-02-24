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

import org.springframework.integration.ws.MarshallingWebServiceOutboundGateway;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.WebServiceMessageSender;

/**
 * The spec for a {@link MarshallingWebServiceOutboundGateway}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.3
 *
 */
public class MarshallingWsOutboundGatewaySpec extends
		BaseWsOutboundGatewaySpec<MarshallingWsOutboundGatewaySpec, MarshallingWebServiceOutboundGateway> {

	protected MarshallingWsOutboundGatewaySpec(WebServiceTemplate template) {
		this.template = template;
	}

	@Override
	protected MarshallingWebServiceOutboundGateway create() {
		if (this.destinationProvider != null) {
			return new MarshallingWebServiceOutboundGateway(this.destinationProvider, this.template);
		}
		else {
			return new MarshallingWebServiceOutboundGateway(this.uri, this.template);
		}
	}

	/**
	 * Spec for a {@link MarshallingWebServiceOutboundGateway} where an external
	 * {@link WebServiceTemplate} is not provided.
	 *
	 */
	public static class MarshallingWsOutboundGatewayNoTemplateSpec
			extends BaseWsOutboundGatewaySpec<MarshallingWsOutboundGatewayNoTemplateSpec,
			MarshallingWebServiceOutboundGateway> {

		protected Marshaller gatewayMarshaller; // NOSONAR

		protected Unmarshaller gatewayUnmarshaller; // NOSONAR

		/**
		 * Configure the marshaller to use.
		 * @param marshaller the marshaller.
		 * @return the spec.
		 */
		public MarshallingWsOutboundGatewayNoTemplateSpec marshaller(Marshaller marshaller) {
			this.gatewayMarshaller = marshaller;
			return this;
		}

		/**
		 * Configure the unmarshaller to use.
		 * @param unmarshaller the unmarshaller.
		 * @return the spec.
		 */
		public MarshallingWsOutboundGatewayNoTemplateSpec unmarshaller(Unmarshaller unmarshaller) {
			this.gatewayUnmarshaller = unmarshaller;
			return this;
		}

		/**
		 * Specify the {@link WebServiceMessageFactory} to use.
		 * @param messageFactory the message factory.
		 * @return the spec.
		 */
		public MarshallingWsOutboundGatewayNoTemplateSpec messageFactory(WebServiceMessageFactory messageFactory) {
			this.webServiceMessageFactory = messageFactory;
			return this;
		}

		/**
		 * Specify the {@link FaultMessageResolver} to use.
		 * @param resolver the resolver.
		 * @return the spec.
		 */
		public MarshallingWsOutboundGatewayNoTemplateSpec faultMessageResolver(FaultMessageResolver resolver) {
			this.faultMessageResolver = resolver;
			return this;
		}

		/**
		 * Specify the {@link WebServiceMessageSender}s to use.
		 * @param senders the senders.
		 * @return the spec.
		 */
		public MarshallingWsOutboundGatewayNoTemplateSpec messageSenders(WebServiceMessageSender... senders) {
			this.messageSenders = Arrays.copyOf(senders, senders.length);
			return this;
		}

		/**
		 * Specify the {@link ClientInterceptor}s to use.
		 * @param interceptors the interceptors.
		 * @return the spec.
		 */
		public MarshallingWsOutboundGatewayNoTemplateSpec interceptors(ClientInterceptor... interceptors) {
			this.gatewayInterceptors = Arrays.copyOf(interceptors, interceptors.length);
			return this;
		}

		@Override
		protected MarshallingWebServiceOutboundGateway create() {
			if (this.destinationProvider != null) {
				return new MarshallingWebServiceOutboundGateway(this.destinationProvider, this.gatewayMarshaller,
						this.gatewayUnmarshaller, this.webServiceMessageFactory);
			}
			else {
				return new MarshallingWebServiceOutboundGateway(this.uri, this.gatewayMarshaller,
						this.gatewayUnmarshaller, this.webServiceMessageFactory);
			}
		}

		@Override
		protected MarshallingWebServiceOutboundGateway assemble(MarshallingWebServiceOutboundGateway gateway) {
			MarshallingWebServiceOutboundGateway assembled = super.assemble(gateway);
			assembled.setFaultMessageResolver(this.faultMessageResolver);
			assembled.setMessageSenders(this.messageSenders);
			assembled.setInterceptors(this.gatewayInterceptors);
			return assembled;
		}

	}

}
