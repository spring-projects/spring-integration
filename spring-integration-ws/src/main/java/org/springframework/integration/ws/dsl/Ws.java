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

import org.springframework.oxm.Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Factory class for web service components.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.3
 *
 */
public final class Ws {

	/**
	 * Create an instance.
	 * @return the spec.
	 */
	public static MarshallingWsInboundGatewaySpec marshallingInboundGateway() {
		return new MarshallingWsInboundGatewaySpec();
	}

	/**
	 * Create an instance with the provided {@link Marshaller} (which must also implement
	 * {@link org.springframework.oxm.Unmarshaller}).
	 * @param marshaller the marshaller.
	 * @return the spec.
	 */
	public static MarshallingWsInboundGatewaySpec marshallingInboundGateway(Marshaller marshaller) {
		MarshallingWsInboundGatewaySpec spec = new MarshallingWsInboundGatewaySpec();
		spec.marshaller(marshaller);
		return spec;
	}

	/**
	 * Create an instance.
	 * @return the spec.
	 */
	public static SimpleWsInboundGatewaySpec simpleInboundGateway() {
		return new SimpleWsInboundGatewaySpec();
	}

	/**
	 * Create an instance with a default {@link WebServiceTemplate}.
	 * @return the spec.
	 */
	public static MarshallingWsOutboundGatewaySpec.MarshallingWsOutboundGatewayNoTemplateSpec
	marshallingOutboundGateway() {
		return new MarshallingWsOutboundGatewaySpec.MarshallingWsOutboundGatewayNoTemplateSpec();
	}

	/**
	 * Create an instance with the provided {@link WebServiceTemplate}.
	 * @param template the template.
	 * @return the spec.
	 */
	public static MarshallingWsOutboundGatewaySpec marshallingOutboundGateway(WebServiceTemplate template) {
		return new MarshallingWsOutboundGatewaySpec(template);
	}

	/**
	 * Create an instance.
	 * @return the spec.
	 */
	public static SimpleWsOutboundGatewaySpec.SimpleWsOutboundGatewayNoTemplateSpec simpleOutboundGateway() {
		return new SimpleWsOutboundGatewaySpec.SimpleWsOutboundGatewayNoTemplateSpec();
	}

	/**
	 * Create an instance with the provided {@link WebServiceTemplate}.
	 * @param template the template.
	 * @return the spec.
	 */
	public static SimpleWsOutboundGatewaySpec simpleOutboundGateway(WebServiceTemplate template) {
		return new SimpleWsOutboundGatewaySpec(template);
	}

	private Ws() {
	}

}
