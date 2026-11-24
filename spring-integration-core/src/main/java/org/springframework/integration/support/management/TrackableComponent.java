/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.support.management;

import org.springframework.integration.support.context.NamedComponent;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
@IntegrationManagedResource
public interface TrackableComponent extends NamedComponent {

	@ManagedOperation
	void setShouldTrack(boolean shouldTrack);

}
