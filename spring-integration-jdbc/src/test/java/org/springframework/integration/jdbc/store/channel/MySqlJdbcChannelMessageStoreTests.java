/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import org.springframework.integration.jdbc.mysql.MySqlContainerTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@ContextConfiguration
public class MySqlJdbcChannelMessageStoreTests extends AbstractJdbcChannelMessageStoreTests
		implements MySqlContainerTest {

}
