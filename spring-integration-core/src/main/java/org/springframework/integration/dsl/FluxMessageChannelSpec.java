/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.dsl;

import org.springframework.integration.channel.FluxMessageChannel;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class FluxMessageChannelSpec extends MessageChannelSpec<FluxMessageChannelSpec, FluxMessageChannel> {

	protected FluxMessageChannelSpec() {
		this.channel = new FluxMessageChannel();
	}

}
