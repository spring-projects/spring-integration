/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
