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

import java.util.concurrent.atomic.AtomicBoolean;

import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import org.springframework.integration.aggregator.agent.grpc.ApplyDecisionResponse;
import org.springframework.integration.aggregator.agent.grpc.ApplyForceCompleteDecisionRequest;
import org.springframework.integration.aggregator.agent.grpc.ApplyMessageDecisionRequest;
import org.springframework.integration.aggregator.agent.grpc.CorrelatingAgentPortGrpc;
import org.springframework.integration.aggregator.agent.grpc.CorrelatingDependencyPortGrpc;
import org.springframework.integration.aggregator.agent.grpc.DecisionOutcome;
import org.springframework.integration.aggregator.agent.grpc.EvaluateForceCompleteRequest;
import org.springframework.integration.aggregator.agent.grpc.EvaluateMessageRequest;
import org.springframework.integration.aggregator.agent.grpc.ForceCompleteAssessment;
import org.springframework.integration.aggregator.agent.grpc.ForceCompleteDecision;
import org.springframework.integration.aggregator.agent.grpc.ForceCompleteRequest;
import org.springframework.integration.aggregator.agent.grpc.ForceCompleteResponse;
import org.springframework.integration.aggregator.agent.grpc.HandleMessageRequest;
import org.springframework.integration.aggregator.agent.grpc.HandleMessageResponse;
import org.springframework.integration.aggregator.agent.grpc.HealthRequest;
import org.springframework.integration.aggregator.agent.grpc.HealthResponse;
import org.springframework.integration.aggregator.agent.grpc.LifecycleRequest;
import org.springframework.integration.aggregator.agent.grpc.LifecycleResponse;
import org.springframework.integration.aggregator.agent.grpc.MessageAssessment;
import org.springframework.integration.aggregator.agent.grpc.MessageDecision;

/**
 * gRPC adapter for {@link EmbabelCorrelatingAgent}.
 *
 * @author OpenAI
 *
 * @since 7.2
 */
public final class EmbabelCorrelatingAgentService extends CorrelatingAgentPortGrpc.CorrelatingAgentPortImplBase {

	private static final int MAX_STALE_RETRIES = 32;

	private final EmbabelCorrelatingAgent agent = new EmbabelCorrelatingAgent();

	private final CorrelatingDependencyPortGrpc.CorrelatingDependencyPortBlockingStub dependencies;

	private final AtomicBoolean running = new AtomicBoolean(true);

	public EmbabelCorrelatingAgentService(Channel dependencyChannel) {
		this.dependencies = CorrelatingDependencyPortGrpc.newBlockingStub(dependencyChannel);
	}

	@Override
	public void handleMessage(HandleMessageRequest request, StreamObserver<HandleMessageResponse> responseObserver) {
		if (!this.running.get()) {
			responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("Correlating agent is stopped")
					.asRuntimeException());
			return;
		}
		try {
			for (int attempt = 1; attempt <= MAX_STALE_RETRIES; attempt++) {
				MessageAssessment assessment = this.dependencies.evaluateMessage(EvaluateMessageRequest.newBuilder()
						.setInvocationId(request.getInvocationId())
						.setMessage(request.getMessage())
						.build());
				MessageDecision decision = this.agent.decide(assessment);
				ApplyDecisionResponse applied = this.dependencies.applyMessageDecision(
						ApplyMessageDecisionRequest.newBuilder()
								.setInvocationId(request.getInvocationId())
								.setMessage(request.getMessage())
								.setExpectedVersion(assessment.getVersion())
								.setDecision(decision)
								.setConditionEvaluated(assessment.getConditionEvaluated())
								.setConditionPresent(assessment.getConditionPresent())
								.setCondition(assessment.getCondition())
								.build());
				if (applied.getOutcome() != DecisionOutcome.STALE) {
					responseObserver.onNext(HandleMessageResponse.newBuilder()
							.setOutcome(applied.getOutcome())
							.setAttempts(attempt)
							.build());
					responseObserver.onCompleted();
					return;
				}
			}
			responseObserver.onError(Status.ABORTED
					.withDescription("Correlating group changed during all decision attempts")
					.asRuntimeException());
		}
		catch (RuntimeException ex) {
			responseObserver.onError(Status.fromThrowable(ex).withCause(ex).asRuntimeException());
		}
	}

	@Override
	public void forceComplete(ForceCompleteRequest request, StreamObserver<ForceCompleteResponse> responseObserver) {
		if (!this.running.get()) {
			responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("Correlating agent is stopped")
					.asRuntimeException());
			return;
		}
		try {
			for (int attempt = 1; attempt <= MAX_STALE_RETRIES; attempt++) {
				ForceCompleteAssessment assessment = this.dependencies.evaluateForceComplete(
						EvaluateForceCompleteRequest.newBuilder()
								.setInvocationId(request.getInvocationId())
								.setGroupId(request.getGroupId())
								.setCandidateTimestamp(request.getCandidateTimestamp())
								.setCandidateLastModified(request.getCandidateLastModified())
								.build());
				ForceCompleteDecision decision = this.agent.decideForceComplete(assessment);
				ApplyDecisionResponse applied = this.dependencies.applyForceCompleteDecision(
						ApplyForceCompleteDecisionRequest.newBuilder()
								.setInvocationId(request.getInvocationId())
								.setGroupId(request.getGroupId())
								.setExpectedVersion(assessment.getVersion())
								.setDecision(decision)
								.build());
				if (applied.getOutcome() != DecisionOutcome.STALE) {
					responseObserver.onNext(ForceCompleteResponse.newBuilder()
							.setOutcome(applied.getOutcome())
							.setAttempts(attempt)
							.build());
					responseObserver.onCompleted();
					return;
				}
			}
			responseObserver.onError(Status.ABORTED
					.withDescription("Correlating group changed during all force-complete attempts")
					.asRuntimeException());
		}
		catch (RuntimeException ex) {
			responseObserver.onError(Status.fromThrowable(ex).withCause(ex).asRuntimeException());
		}
	}

	@Override
	public void health(HealthRequest request, StreamObserver<HealthResponse> responseObserver) {
		responseObserver.onNext(HealthResponse.newBuilder()
				.setServing(this.running.get())
				.setAgent(EmbabelCorrelatingAgent.class.getName())
				.build());
		responseObserver.onCompleted();
	}

	@Override
	public void start(LifecycleRequest request, StreamObserver<LifecycleResponse> responseObserver) {
		this.running.set(true);
		respondWithLifecycleState(responseObserver);
	}

	@Override
	public void stop(LifecycleRequest request, StreamObserver<LifecycleResponse> responseObserver) {
		this.running.set(false);
		respondWithLifecycleState(responseObserver);
	}

	private void respondWithLifecycleState(StreamObserver<LifecycleResponse> responseObserver) {
		responseObserver.onNext(LifecycleResponse.newBuilder().setRunning(this.running.get()).build());
		responseObserver.onCompleted();
	}

}
