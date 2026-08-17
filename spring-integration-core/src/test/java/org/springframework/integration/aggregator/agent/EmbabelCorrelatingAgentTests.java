/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.aggregator.agent;

import com.embabel.agent.api.annotation.Agent;
import org.junit.jupiter.api.Test;

import org.springframework.integration.aggregator.agent.grpc.ForceCompleteAssessment;
import org.springframework.integration.aggregator.agent.grpc.ForceCompleteDecision;
import org.springframework.integration.aggregator.agent.grpc.MessageAssessment;
import org.springframework.integration.aggregator.agent.grpc.MessageDecision;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbabelCorrelatingAgent} actions.
 *
 * @author OpenAI
 *
 * @since 7.2
 */
class EmbabelCorrelatingAgentTests {

	private final EmbabelCorrelatingAgent agent = new EmbabelCorrelatingAgent();

	@Test
	void isAnEmbabelAgent() {
		assertThat(EmbabelCorrelatingAgent.class).hasAnnotation(Agent.class);
	}

	@Test
	void decidesNormalMessageTransitions() {
		assertThat(this.agent.decide(MessageAssessment.newBuilder().setCanAdd(true).build()))
				.isEqualTo(MessageDecision.STORE_AND_WAIT);
		assertThat(this.agent.decide(MessageAssessment.newBuilder()
				.setCanAdd(true)
				.setCanReleaseAfterStore(true)
				.build()))
				.isEqualTo(MessageDecision.STORE_AND_RELEASE);
		assertThat(this.agent.decide(MessageAssessment.newBuilder().setGroupComplete(true).build()))
				.isEqualTo(MessageDecision.DISCARD);
	}

	@Test
	void decidesForceCompleteTransitions() {
		assertThat(this.agent.decideForceComplete(ForceCompleteAssessment.newBuilder().build()))
				.isEqualTo(ForceCompleteDecision.IGNORE);
		assertThat(this.agent.decideForceComplete(ForceCompleteAssessment.newBuilder()
				.setEligible(true)
				.setEmpty(true)
				.build()))
				.isEqualTo(ForceCompleteDecision.REMOVE_EMPTY);
		assertThat(this.agent.decideForceComplete(ForceCompleteAssessment.newBuilder()
				.setEligible(true)
				.setCanRelease(true)
				.build()))
				.isEqualTo(ForceCompleteDecision.RELEASE);
	}

}
