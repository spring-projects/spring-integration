/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import org.springframework.integration.jdbc.postgres.PostgresContainerTest;
import org.springframework.test.context.ContextConfiguration;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
@ContextConfiguration
public class PostgresTxTimeoutMessageStoreTests extends AbstractTxTimeoutMessageStoreTests implements PostgresContainerTest {

}
