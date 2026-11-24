/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.dsl;

import org.springframework.integration.channel.RendezvousChannel;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class RendezvousChannelSpec extends MessageChannelSpec<RendezvousChannelSpec, RendezvousChannel> {

	protected RendezvousChannelSpec() {
		this.channel = new RendezvousChannel();
	}

}
