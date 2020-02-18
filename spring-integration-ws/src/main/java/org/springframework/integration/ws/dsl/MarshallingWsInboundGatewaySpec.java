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

import org.springframework.integration.ws.MarshallingWebServiceInboundGateway;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;

/**
 * The spec for a {@link MarshallingWebServiceInboundGateway}.
 *
 * @author Gary Russell
 * @since 5.3
 *
 */
public class MarshallingWsInboundGatewaySpec extends BaseWsInboundGatewaySpec<MarshallingWsInboundGatewaySpec,
	MarshallingWebServiceInboundGateway> {

	protected Marshaller gatewayMarshaller; // NOSONAR

	protected Unmarshaller gatewayUnmarshaller; // NOSONAR

	/**
	 * Specify a marshaller to use.
	 * @param marshaller the marshaller.
	 * @return the spec.
	 */
	public MarshallingWsInboundGatewaySpec marshaller(Marshaller marshaller) {
		this.gatewayMarshaller = marshaller;
		return this;
	}

	/**
	 * Specify an unmarshaller to use. Required if the {@link #gatewayMarshaller} is not also
	 * an {@link Unmarshaller}.
	 * @param unmarshaller the unmarshaller.
	 * @return the spec.
	 */
	public MarshallingWsInboundGatewaySpec unmarshaller(Unmarshaller unmarshaller) {
		this.gatewayUnmarshaller = unmarshaller;
		return this;
	}

	@Override
	protected MarshallingWebServiceInboundGateway create() {
		if (this.gatewayUnmarshaller != null) {
			return new MarshallingWebServiceInboundGateway(this.gatewayMarshaller, this.gatewayUnmarshaller);
		}
		else {
			return new MarshallingWebServiceInboundGateway(this.gatewayMarshaller);
		}
	}

}
