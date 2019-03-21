/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.integration.jpa.dsl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.outbound.JpaOutboundGateway;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.integration.jpa.support.parametersource.ParameterSourceFactory;

/**
 * The base {@link MessageHandlerSpec} for JPA Outbound endpoints.
 *
 * @param <S> the target {@link JpaBaseOutboundEndpointSpec} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class JpaBaseOutboundEndpointSpec<S extends JpaBaseOutboundEndpointSpec<S>>
		extends MessageHandlerSpec<S, JpaOutboundGateway>
		implements ComponentsRegistration {

	private final List<JpaParameter> jpaParameters = new LinkedList<>();

	protected final JpaExecutor jpaExecutor; // NOSONAR

	protected JpaBaseOutboundEndpointSpec(JpaExecutor jpaExecutor) {
		this.jpaExecutor = jpaExecutor;
		this.jpaExecutor.setJpaParameters(this.jpaParameters);
		this.target = new JpaOutboundGateway(this.jpaExecutor);
	}

	/**
	 * Specify the class type which is being used for retrieving entities from the database.
	 * @param entityClass the entity {@link Class} to use
	 * @return the spec
	 */
	public S entityClass(Class<?> entityClass) {
		this.jpaExecutor.setEntityClass(entityClass);
		return _this();
	}

	/**
	 * Specify a JPA query to perform persistent operation.
	 * @param jpaQuery the JPA query to use.
	 * @return the spec
	 */
	public S jpaQuery(String jpaQuery) {
		this.jpaExecutor.setJpaQuery(jpaQuery);
		return _this();
	}

	/**
	 * Specify a native SQL query to perform persistent operation.
	 * @param nativeQuery the native SQL query to use.
	 * @return the spec
	 */
	public S nativeQuery(String nativeQuery) {
		this.jpaExecutor.setNativeQuery(nativeQuery);
		return _this();
	}

	/**
	 * Specify a name a named JPQL based query or a native SQL query.
	 * @param namedQuery the name of the pre-configured query.
	 * @return the spec
	 */
	public S namedQuery(String namedQuery) {
		this.jpaExecutor.setNamedQuery(namedQuery);
		return _this();
	}

	/**
	 * Specify a {@link ParameterSourceFactory} to populate query parameters at runtime against request message.
	 * @param parameterSourceFactory the {@link ParameterSourceFactory} to use.
	 * @return the spec
	 */
	public S parameterSourceFactory(ParameterSourceFactory parameterSourceFactory) {
		this.jpaExecutor.setParameterSourceFactory(parameterSourceFactory);
		return _this();
	}

	/**
	 * Add a value for indexed query parameter.
	 * @param value the value for query parameter by index
	 * @return the spec
	 */
	public S parameter(Object value) {
		return parameter(new JpaParameter(value, null));
	}

	/**
	 * Add a value for named parameter in the query.
	 * @param name the name of the query parameter
	 * @param value the value for query parameter by name
	 * @return the spec
	 */
	public S parameter(String name, Object value) {
		return parameter(new JpaParameter(name, value, null));
	}

	/**
	 * Add a SpEL expression for indexed parameter in the query.
	 * @param expression the SpEL expression for query parameter by index
	 * @return the spec
	 */
	public S parameterExpression(String expression) {
		return parameter(new JpaParameter(null, expression));
	}

	/**
	 * Add a SpEL expression for named parameter in the query.
	 * @param name the name of the query parameter
	 * @param expression the SpEL expression for query parameter by name
	 * @return the spec
	 */
	public S parameterExpression(String name, String expression) {
		return parameter(new JpaParameter(name, null, expression));
	}

	public S parameter(JpaParameter jpaParameter) {
		this.jpaParameters.add(jpaParameter);
		return _this();
	}

	/**
	 * Indicates that whether only the payload of the passed in {@code Message} will be
	 * used as a source of parameters. The is 'true' by default because as a default a
	 * {@link org.springframework.integration.jpa.support.parametersource.BeanPropertyParameterSourceFactory}
	 * implementation is used for the sqlParameterSourceFactory property.
	 * @param usePayloadAsParameterSource the {@code boolean} flag to indicate if use
	 * {@code payload} as a source of parameter values or not.
	 * @return the spec
	 */
	public S usePayloadAsParameterSource(Boolean usePayloadAsParameterSource) {
		this.jpaExecutor.setUsePayloadAsParameterSource(usePayloadAsParameterSource);
		return _this();
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return Collections.singletonMap(this.jpaExecutor, null);
	}

}
