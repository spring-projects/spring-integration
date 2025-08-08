/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.storedproc;

/**
 *
 * @author Gunnar Hillert
 *
 */
public interface CreateUser {

	void createUser(User user);

}
