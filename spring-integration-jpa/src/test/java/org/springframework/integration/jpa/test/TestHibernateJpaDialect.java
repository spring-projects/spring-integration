/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.jpa.test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.PersistenceException;

import org.springframework.orm.jpa.vendor.HibernateJpaDialect;

/**
 * @author Artem Bilan
 *
 * @since 3.0
 */
@SuppressWarnings("serial")
public class TestHibernateJpaDialect extends HibernateJpaDialect {

	@Override
	public Object prepareTransaction(EntityManager entityManager, boolean readOnly, String name)
			throws PersistenceException {

		entityManager.setFlushMode(FlushModeType.COMMIT);
		return super.prepareTransaction(entityManager, readOnly, name);
	}

}
