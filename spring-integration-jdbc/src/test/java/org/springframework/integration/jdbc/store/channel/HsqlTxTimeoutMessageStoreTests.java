/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import org.springframework.integration.test.condition.LongRunningTest;
import org.springframework.test.context.ContextConfiguration;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
@LongRunningTest
@ContextConfiguration
public class HsqlTxTimeoutMessageStoreTests extends AbstractTxTimeoutMessageStoreTests {

}
