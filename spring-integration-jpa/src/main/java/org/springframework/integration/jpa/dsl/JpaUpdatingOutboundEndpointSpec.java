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

import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.support.PersistMode;

/**
 * A {@link JpaBaseOutboundEndpointSpec} extension for the {@code updating}
 * {@link org.springframework.integration.jpa.outbound.JpaOutboundGateway} mode.
 * The {@code outbound-channel-adapter} is achievable through an internal {@code producesReply} option.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class JpaUpdatingOutboundEndpointSpec extends JpaBaseOutboundEndpointSpec<JpaUpdatingOutboundEndpointSpec> {

	protected JpaUpdatingOutboundEndpointSpec(JpaExecutor jpaExecutor) {
		super(jpaExecutor);
	}

	protected JpaUpdatingOutboundEndpointSpec producesReply(boolean producesReply) {
		this.target.setProducesReply(producesReply);
		if (producesReply) {
			this.target.setRequiresReply(true);
		}
		return this;
	}

	/**
	 * Specify a {@link PersistMode} for the gateway.
	 * Defaults to {@link PersistMode#MERGE}.
	 * @param persistMode the {@link PersistMode} to use.
	 * @return the spec
	 */
	public JpaUpdatingOutboundEndpointSpec persistMode(PersistMode persistMode) {
		this.jpaExecutor.setPersistMode(persistMode);
		return this;
	}

	/**
	 * If set to {@code true} the {@link jakarta.persistence.EntityManager#flush()} will be called
	 * after persistence operation.
	 * Has the same effect, if the {@link #flushSize} is specified to {@code 1}.
	 * For convenience in cases when the provided entity to persist is not an instance of {@link Iterable}.
	 * @param flush defaults to {@code false}.
	 * @return the spec
	 */
	public JpaUpdatingOutboundEndpointSpec flush(boolean flush) {
		this.jpaExecutor.setFlush(flush);
		return this;
	}

	/**
	 * If the provided value is greater than {@code 0}, then {@link jakarta.persistence.EntityManager#flush()}
	 * will be called after persistence operations as well as within batch operations.
	 * This property has precedence over the {@link #flush}, if it is specified to a value greater than {@code 0}.
	 * If the entity to persist is not an instance of {@link Iterable} and this property is greater than {@code 0},
	 * then the entity will be flushed as if the {@link #flush} attribute was set to {@code true}.
	 * @param flushSize defaults to {@code 0}.
	 * @return the spec
	 */
	public JpaUpdatingOutboundEndpointSpec flushSize(int flushSize) {
		this.jpaExecutor.setFlushSize(flushSize);
		return this;
	}

	/**
	 * If set to {@code true} the {@link jakarta.persistence.EntityManager#clear()} will be called,
	 * and only if the {@link jakarta.persistence.EntityManager#flush()}
	 * was called after performing persistence operations.
	 * @param clearOnFlush defaults to {@code false}.
	 * @return the spec
	 */
	public JpaUpdatingOutboundEndpointSpec clearOnFlush(boolean clearOnFlush) {
		this.jpaExecutor.setClearOnFlush(clearOnFlush);
		return this;
	}

}
