/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.jms.dsl;

import jakarta.jms.ConnectionFactory;

import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.JmsDestinationAccessor;
import org.springframework.lang.Nullable;

/**
 * A base {@link IntegrationComponentSpec} for {@link JmsDestinationAccessor}s.
 *
 * @param <S> the target {@link JmsDestinationAccessorSpec} implementation type.
 * @param <A> the target {@link JmsDestinationAccessor} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class JmsDestinationAccessorSpec<S extends JmsDestinationAccessorSpec<S, A>, A extends JmsDestinationAccessor>
		extends IntegrationComponentSpec<S, A> {

	protected JmsDestinationAccessorSpec(A accessor) {
		this.target = accessor;
	}

	protected S connectionFactory(ConnectionFactory connectionFactory) {
		this.target.setConnectionFactory(connectionFactory);
		return _this();
	}

	@Override
	public S id(@Nullable String id) { // NOSONAR - not useless, increases visibility
		return super.id(id);
	}

	/**
	 * A {@link DestinationResolver} to use.
	 * @param destinationResolver the {@link DestinationResolver} to use.
	 * @return the spec
	 * @see JmsDestinationAccessor#setDestinationResolver
	 */
	public S destinationResolver(DestinationResolver destinationResolver) {
		this.target.setDestinationResolver(destinationResolver);
		return _this();
	}

	/**
	 * A {@code pubSubDomain} flag.
	 * @param pubSubDomain the {@code pubSubDomain} flag.
	 * @return the spec
	 * @see JmsDestinationAccessor#setPubSubDomain(boolean)
	 */
	public S pubSubDomain(boolean pubSubDomain) {
		target.setPubSubDomain(pubSubDomain);
		return _this();
	}

	/**
	 * A session acknowledgement mode.
	 * @param sessionAcknowledgeMode the acknowledgement mode constant
	 * @return the spec
	 * @see jakarta.jms.Session#AUTO_ACKNOWLEDGE etc.
	 * @see JmsDestinationAccessor#setSessionAcknowledgeMode
	 */
	public S sessionAcknowledgeMode(int sessionAcknowledgeMode) {
		this.target.setSessionAcknowledgeMode(sessionAcknowledgeMode);
		return _this();
	}

	/**
	 * A session acknowledgement mode name.
	 * @param constantName the name of the {@link jakarta.jms.Session} acknowledge mode constant.
	 * @return the spec.
	 * @see JmsDestinationAccessor#setSessionAcknowledgeModeName
	 */
	public S sessionAcknowledgeModeName(String constantName) {
		target.setSessionAcknowledgeModeName(constantName);
		return _this();
	}

	/**
	 * A session transaction mode.
	 * @param sessionTransacted the transaction mode.
	 * @return the spec.
	 * @see JmsDestinationAccessor#setSessionTransacted
	 */
	public S sessionTransacted(boolean sessionTransacted) {
		this.target.setSessionTransacted(sessionTransacted);
		return _this();
	}

}
