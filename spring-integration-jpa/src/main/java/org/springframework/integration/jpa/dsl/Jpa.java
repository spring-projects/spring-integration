/*
 * Copyright 2016-2021 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;

/**
 * Factory class for JPA components.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class Jpa {

	/**
	 * Create a {@link JpaInboundChannelAdapterSpec} builder instance
	 * based on the provided {@link EntityManagerFactory}.
	 * @param entityManagerFactory the {@link EntityManagerFactory} to use
	 * @return the JpaInboundChannelAdapterSpec instance
	 */
	public static JpaInboundChannelAdapterSpec inboundAdapter(EntityManagerFactory entityManagerFactory) {
		return inboundAdapter(new JpaExecutor(entityManagerFactory));
	}

	/**
	 * Create a {@link JpaInboundChannelAdapterSpec} builder instance
	 * based on the provided {@link EntityManager}.
	 * @param entityManager the {@link EntityManager} to use
	 * @return the JpaInboundChannelAdapterSpec instance
	 */
	public static JpaInboundChannelAdapterSpec inboundAdapter(EntityManager entityManager) {
		return inboundAdapter(new JpaExecutor(entityManager));
	}

	/**
	 * Create a {@link JpaInboundChannelAdapterSpec} builder instance
	 * based on the provided {@link JpaOperations}.
	 * @param jpaOperations the {@link JpaOperations} to use
	 * @return the JpaInboundChannelAdapterSpec instance
	 */
	public static JpaInboundChannelAdapterSpec inboundAdapter(JpaOperations jpaOperations) {
		return inboundAdapter(new JpaExecutor(jpaOperations));
	}

	private static JpaInboundChannelAdapterSpec inboundAdapter(JpaExecutor jpaExecutor) {
		return new JpaInboundChannelAdapterSpec(jpaExecutor);
	}

	/**
	 * Create a {@link JpaUpdatingOutboundEndpointSpec} builder instance for one-way adapter
	 * based on the provided {@link EntityManagerFactory}.
	 * @param entityManagerFactory the {@link EntityManagerFactory} to use
	 * @return the JpaUpdatingOutboundEndpointSpec instance
	 */
	public static JpaUpdatingOutboundEndpointSpec outboundAdapter(EntityManagerFactory entityManagerFactory) {
		return outboundAdapter(new JpaExecutor(entityManagerFactory));
	}

	/**
	 * Create a {@link JpaUpdatingOutboundEndpointSpec} builder instance for one-way adapter
	 * based on the provided {@link EntityManager}.
	 * @param entityManager the {@link EntityManager} to use
	 * @return the JpaUpdatingOutboundEndpointSpec instance
	 */
	public static JpaUpdatingOutboundEndpointSpec outboundAdapter(EntityManager entityManager) {
		return outboundAdapter(new JpaExecutor(entityManager));
	}

	/**
	 * Create a {@link JpaUpdatingOutboundEndpointSpec} builder instance for one-way adapter
	 * based on the provided {@link JpaOperations}.
	 * @param jpaOperations the {@link JpaOperations} to use
	 * @return the JpaUpdatingOutboundEndpointSpec instance
	 */
	public static JpaUpdatingOutboundEndpointSpec outboundAdapter(JpaOperations jpaOperations) {
		return outboundAdapter(new JpaExecutor(jpaOperations));
	}

	private static JpaUpdatingOutboundEndpointSpec outboundAdapter(JpaExecutor jpaExecutor) {
		return new JpaUpdatingOutboundEndpointSpec(jpaExecutor)
				.producesReply(false);
	}

	/**
	 * Create a {@link JpaUpdatingOutboundEndpointSpec} builder instance for request-reply gateway
	 * based on the provided {@link EntityManagerFactory}.
	 * @param entityManagerFactory the {@link EntityManagerFactory} to use
	 * @return the JpaUpdatingOutboundEndpointSpec instance
	 */
	public static JpaUpdatingOutboundEndpointSpec updatingGateway(EntityManagerFactory entityManagerFactory) {
		return updatingGateway(new JpaExecutor(entityManagerFactory));
	}

	/**
	 * Create a {@link JpaUpdatingOutboundEndpointSpec} builder instance for request-reply gateway
	 * based on the provided {@link EntityManager}.
	 * @param entityManager the {@link EntityManager} to use
	 * @return the JpaUpdatingOutboundEndpointSpec instance
	 */
	public static JpaUpdatingOutboundEndpointSpec updatingGateway(EntityManager entityManager) {
		return updatingGateway(new JpaExecutor(entityManager));
	}

	/**
	 * Create a {@link JpaUpdatingOutboundEndpointSpec} builder instance for request-reply gateway
	 * based on the provided {@link JpaOperations}.
	 * @param jpaOperations the {@link JpaOperations} to use
	 * @return the JpaUpdatingOutboundEndpointSpec instance
	 */
	public static JpaUpdatingOutboundEndpointSpec updatingGateway(JpaOperations jpaOperations) {
		return updatingGateway(new JpaExecutor(jpaOperations));
	}

	private static JpaUpdatingOutboundEndpointSpec updatingGateway(JpaExecutor jpaExecutor) {
		return new JpaUpdatingOutboundEndpointSpec(jpaExecutor)
				.producesReply(true);
	}

	/**
	 * Create a {@link JpaRetrievingOutboundGatewaySpec} builder instance for request-reply gateway
	 * based on the provided {@link EntityManagerFactory}.
	 * @param entityManagerFactory the {@link EntityManagerFactory} to use
	 * @return the JpaRetrievingOutboundGatewaySpec instance
	 */
	public static JpaRetrievingOutboundGatewaySpec retrievingGateway(EntityManagerFactory entityManagerFactory) {
		return retrievingGateway(new JpaExecutor(entityManagerFactory));
	}

	/**
	 * Create a {@link JpaRetrievingOutboundGatewaySpec} builder instance for request-reply gateway
	 * based on the provided {@link EntityManager}.
	 * @param entityManager the {@link EntityManager} to use
	 * @return the JpaRetrievingOutboundGatewaySpec instance
	 */
	public static JpaRetrievingOutboundGatewaySpec retrievingGateway(EntityManager entityManager) {
		return retrievingGateway(new JpaExecutor(entityManager));
	}

	/**
	 * Create a {@link JpaRetrievingOutboundGatewaySpec} builder instance for request-reply gateway
	 * based on the provided {@link JpaOperations}.
	 * @param jpaOperations the {@link JpaOperations} to use
	 * @return the JpaRetrievingOutboundGatewaySpec instance
	 */
	public static JpaRetrievingOutboundGatewaySpec retrievingGateway(JpaOperations jpaOperations) {
		return retrievingGateway(new JpaExecutor(jpaOperations));
	}

	private static JpaRetrievingOutboundGatewaySpec retrievingGateway(JpaExecutor jpaExecutor) {
		return new JpaRetrievingOutboundGatewaySpec(jpaExecutor);
	}

	private Jpa() {
	}

}
