/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.jmx.inbound;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;

/**
 * @author Stuart Williams
 *
 * @since 7.0
 *
 */
@FunctionalInterface
public interface MBeanObjectConverter {

	/**
	 * @param connection The connection.
	 * @param instance The instance.
	 * @return The mapped object instance.
	 */
	Object convert(MBeanServerConnection connection, ObjectInstance instance);

}
