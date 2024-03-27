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

package org.springframework.integration.jpa.core;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.util.Assert;

/**
 *
 * @author Gunnar Hillert
 *
 * @since 2.2
 *
 */
abstract class AbstractJpaOperations implements JpaOperations, InitializingBean {

	private EntityManager entityManager;

	private EntityManagerFactory entityManagerFactory;

	public void setEntityManager(EntityManager entityManager) {
		Assert.notNull(entityManager, "The provided entityManager must not be null.");
		this.entityManager = entityManager;
	}

	protected EntityManager getEntityManager() {
		return this.entityManager;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		Assert.notNull(entityManagerFactory, "The provided entityManagerFactory must not be null.");
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public final void afterPropertiesSet() {
		this.onInit();
	}

	/**
	 * Subclasses may implement this for initialization logic.
	 */

	protected void onInit() {

		if (this.entityManager == null && this.entityManagerFactory != null) {
			this.entityManager = SharedEntityManagerCreator.createSharedEntityManager(this.entityManagerFactory);
		}

		Assert.notNull(this.entityManager, "The entityManager is null. Please set " +
				"either the entityManager or the entityManagerFactory.");

	}

	@Override
	public void flush() {
		this.entityManager.flush();
	}

}
