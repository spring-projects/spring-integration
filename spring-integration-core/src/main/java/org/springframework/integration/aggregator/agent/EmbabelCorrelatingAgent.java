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

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;

import org.springframework.integration.aggregator.agent.grpc.ForceCompleteAssessment;
import org.springframework.integration.aggregator.agent.grpc.ForceCompleteDecision;
import org.springframework.integration.aggregator.agent.grpc.MessageAssessment;
import org.springframework.integration.aggregator.agent.grpc.MessageDecision;

/**
 * Embabel agent that owns correlating message decisions. Its domain is restricted
 * to the generated port model; Spring Integration dependencies remain behind gRPC.
 *
 * @author OpenAI
 *
 * @since 7.2
 */
@Agent(description = "Decides how correlated Spring Integration message groups advance")
public final class EmbabelCorrelatingAgent {

	@Action(description = "Choose whether an incoming message is stored, released, or discarded")
	@AchievesGoal(description = "Produce a validated incoming-message correlation decision")
	public MessageDecision decide(MessageAssessment assessment) {
		if (assessment.getGroupComplete() || (!assessment.getMessagePresent() && !assessment.getCanAdd())) {
			return MessageDecision.DISCARD;
		}
		return assessment.getCanReleaseAfterStore()
				? MessageDecision.STORE_AND_RELEASE
				: MessageDecision.STORE_AND_WAIT;
	}

	@Action(description = "Choose how an eligible timed-out message group is completed")
	@AchievesGoal(description = "Produce a validated force-completion decision")
	public ForceCompleteDecision decideForceComplete(ForceCompleteAssessment assessment) {
		if (!assessment.getEligible()) {
			return ForceCompleteDecision.IGNORE;
		}
		if (assessment.getEmpty()) {
			return ForceCompleteDecision.REMOVE_EMPTY;
		}
		return assessment.getCanRelease()
				? ForceCompleteDecision.RELEASE
				: ForceCompleteDecision.EXPIRE;
	}

}
