/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.kafka.dsl;

import org.springframework.integration.kafka.channel.PollableKafkaChannel;
import org.springframework.integration.kafka.inbound.KafkaMessageSource;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Spec for a pollable channel.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public class KafkaPollableChannelSpec extends AbstractKafkaChannelSpec<KafkaPollableChannelSpec, PollableKafkaChannel> {

	protected KafkaPollableChannelSpec(KafkaTemplate<?, ?> template, KafkaMessageSource<?, ?> source) {
		this.channel = new PollableKafkaChannel(template, source);
	}

}
