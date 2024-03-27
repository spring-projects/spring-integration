/*
 * Copyright 2020-2024 the original author or authors.
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
 * @author Artem Bilan
 *
 * @since 5.3
 *
 */
public class MarshallingWsInboundGatewaySpec
		extends BaseWsInboundGatewaySpec<MarshallingWsInboundGatewaySpec, MarshallingWebServiceInboundGateway> {

	protected MarshallingWsInboundGatewaySpec() {
		super(new MarshallingWebServiceInboundGateway());
	}

	/**
	 * Specify a marshaller to use.
	 * @param marshaller the marshaller.
	 * @return the spec.
	 */
	public MarshallingWsInboundGatewaySpec marshaller(Marshaller marshaller) {
		this.target.setMarshaller(marshaller);
		if (marshaller instanceof Unmarshaller unmarshaller) {
			return unmarshaller(unmarshaller);
		}
		return this;
	}

	/**
	 * Specify an unmarshaller to use. Required if the {@link #marshaller} is not also
	 * an {@link Unmarshaller}.
	 * @param unmarshaller the unmarshaller.
	 * @return the spec.
	 */
	public MarshallingWsInboundGatewaySpec unmarshaller(Unmarshaller unmarshaller) {
		this.target.setUnmarshaller(unmarshaller);
		return this;
	}

}
