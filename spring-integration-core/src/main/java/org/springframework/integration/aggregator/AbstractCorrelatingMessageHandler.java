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

package org.springframework.integration.aggregator;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.BiFunction;

import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.aopalliance.aop.Advice;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.Lifecycle;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.aggregator.agent.CorrelatingMessageMapper;
import org.springframework.integration.aggregator.agent.CorrelatingPayloadCodec;
import org.springframework.integration.aggregator.agent.EmbabelCorrelatingAgentService;
import org.springframework.integration.aggregator.agent.JavaSerializationCorrelatingPayloadCodec;
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
import org.springframework.integration.aggregator.agent.grpc.LifecycleRequest;
import org.springframework.integration.aggregator.agent.grpc.MessageAssessment;
import org.springframework.integration.aggregator.agent.grpc.MessageDecision;
import org.springframework.integration.aggregator.agent.grpc.MessageEnvelope;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.DiscardingMessageHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.store.UniqueExpiryCallback;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Abstract Message handler that holds a buffer of correlated messages in a
 * {@link org.springframework.integration.store.MessageStore}.
 * This class takes care of correlated groups of messages
 * that can be completed in batches. It is useful for custom implementation of
 * MessageHandlers that require correlation and is used as a base class for Aggregator -
 * {@link AggregatingMessageHandler} and Resequencer - {@link ResequencingMessageHandler},
 * or custom implementations requiring correlation.
 * <p>
 * To customize this handler inject {@link CorrelationStrategy},
 * {@link ReleaseStrategy}, and {@link MessageGroupProcessor} implementations as
 * you require.
 * <p>
 * By default, the {@link CorrelationStrategy} will be a
 * {@link HeaderAttributeCorrelationStrategy} and the {@link ReleaseStrategy} will be a
 * {@link SequenceSizeReleaseStrategy}.
 * <p>
 * Use proper {@link CorrelationStrategy} for cases when same
 * {@link org.springframework.integration.store.MessageStore} is used
 * for multiple handlers to ensure uniqueness of message groups across handlers.
 * <p>
 * When the {@link #expireTimeout} is greater than 0, groups which are older than this timeout
 * are purged from the store on start up (or when {@link #purgeOrphanedGroups()} is called).
 * If {@link #expireDuration} is provided, the task is scheduled to perform
 * {@link #purgeOrphanedGroups()} periodically.
 *
 * @author Iwein Fuld
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author David Liu
 * @author Enrique Rodriguez
 * @author Meherzad Lahewala
 * @author Jayadev Sirimamilla
 * @author Ngoc Nhan
 *
 * @since 2.0
 */
public abstract class AbstractCorrelatingMessageHandler extends AbstractMessageProducingHandler
		implements DiscardingMessageHandler, ApplicationEventPublisherAware, ManageableLifecycle {

	private final Comparator<Message<?>> sequenceNumberComparator = new MessageSequenceComparator();

	private final Map<UUID, ScheduledFuture<?>> expireGroupScheduledFutures = new ConcurrentHashMap<>();

	private Duration correlatingAgentDeadline = Duration.ofSeconds(30);

	private CorrelatingPayloadCodec payloadCodec = new JavaSerializationCorrelatingPayloadCodec();

	private ObjectInputFilter deserializationFilter = JavaSerializationCorrelatingPayloadCodec.defaultFilter();

	@Nullable
	private Channel correlatingAgentChannel;

	@Nullable
	private ManagedChannel managedCorrelatingAgentChannel;

	@Nullable
	private Server correlatingAgentServer;

	private CorrelatingAgentPortGrpc.@Nullable CorrelatingAgentPortBlockingStub correlatingAgent;

	@Nullable
	private CorrelatingDependencyGateway correlatingDependencyGateway;

	private MessageGroupProcessor outputProcessor;

	@SuppressWarnings("NullAway.Init")
	private MessageGroupStore messageStore;

	private CorrelationStrategy correlationStrategy;

	private ReleaseStrategy releaseStrategy;

	private boolean releaseStrategySet;

	@Nullable
	private MessageChannel discardChannel;

	@Nullable
	private String discardChannelName;

	private boolean sendPartialResultOnExpiry;

	private boolean discardIndividuallyOnExpiry = true;

	private boolean sequenceAware;

	private LockRegistry<?> lockRegistry = new DefaultLockRegistry();

	private boolean lockRegistrySet = false;

	private long minimumTimeoutForEmptyGroups;

	private boolean releasePartialSequences;

	@Nullable
	private Expression groupTimeoutExpression;

	@Nullable
	private List<Advice> forceReleaseAdviceChain;

	private long expireTimeout;

	@Nullable
	private Duration expireDuration;

	private MessageGroupProcessor forceReleaseProcessor = new ForceReleaseMessageGroupProcessor();

	@SuppressWarnings("NullAway.Init")
	private EvaluationContext evaluationContext;

	@Nullable
	private ApplicationEventPublisher applicationEventPublisher;

	private boolean expireGroupsUponTimeout = true;

	private boolean popSequence = true;

	private boolean releaseLockBeforeSend;

	private volatile boolean running;

	@Nullable
	private BiFunction<Message<?>, String, String> groupConditionSupplier;

	public AbstractCorrelatingMessageHandler(MessageGroupProcessor processor, MessageGroupStore store,
			@Nullable CorrelationStrategy correlationStrategy, @Nullable ReleaseStrategy releaseStrategy) {

		Assert.notNull(processor, "'processor' must not be null");
		Assert.notNull(store, "'store' must not be null");

		setMessageStore(store);
		this.outputProcessor = processor;

		this.correlationStrategy =
				correlationStrategy == null
						? new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID)
						: correlationStrategy;

		this.releaseStrategy =
				releaseStrategy == null
						? new SimpleSequenceSizeReleaseStrategy()
						: releaseStrategy;

		this.releaseStrategySet = releaseStrategy != null;
		this.sequenceAware = this.releaseStrategy instanceof SequenceSizeReleaseStrategy;
	}

	public AbstractCorrelatingMessageHandler(MessageGroupProcessor processor, MessageGroupStore store) {
		this(processor, store, null, null);
	}

	public AbstractCorrelatingMessageHandler(MessageGroupProcessor processor) {
		this(processor, new SimpleMessageStore(0), null, null);
	}

	public void setLockRegistry(LockRegistry<?> lockRegistry) {
		Assert.isTrue(!this.lockRegistrySet, "'this.lockRegistry' can not be reset once its been set");
		Assert.notNull(lockRegistry, "'lockRegistry' must not be null");
		this.lockRegistry = lockRegistry;
		this.lockRegistrySet = true;
	}

	/**
	 * Configure an externally managed channel for the correlating agent port.
	 * When omitted, an in-process Embabel agent and dependency gateway are created.
	 * @param channel the agent channel
	 * @since 7.2
	 */
	public void setCorrelatingAgentChannel(Channel channel) {
		Assert.state(this.correlatingAgent == null, "The correlating agent has already been initialized");
		Assert.notNull(channel, "'channel' must not be null");
		this.correlatingAgentChannel = channel;
	}

	/**
	 * Return the gRPC dependency port that gives an externally hosted correlating agent
	 * access to this handler's strategies, store, processor, channels, locks, timeouts,
	 * and application events. Add this service to a gRPC server reachable by the agent.
	 * @return the dependency port service
	 * @since 7.2
	 */
	public BindableService getCorrelatingDependencyPort() {
		initializeCorrelatingAgent();
		CorrelatingDependencyGateway dependencyGateway = this.correlatingDependencyGateway;
		Assert.state(dependencyGateway != null, "The correlating dependency port could not be initialized");
		return dependencyGateway;
	}

	/**
	 * Configure the deadline applied to each agent invocation.
	 * @param deadline the positive deadline
	 * @since 7.2
	 */
	public void setCorrelatingAgentDeadline(Duration deadline) {
		Assert.notNull(deadline, "'deadline' must not be null");
		Assert.isTrue(!deadline.isNegative() && !deadline.isZero(), "'deadline' must be greater than zero");
		this.correlatingAgentDeadline = deadline;
	}

	/**
	 * Configure the payload codec used at the gRPC boundary.
	 * @param payloadCodec the payload codec
	 * @since 7.2
	 */
	public void setPayloadCodec(CorrelatingPayloadCodec payloadCodec) {
		Assert.notNull(payloadCodec, "'payloadCodec' must not be null");
		this.payloadCodec = payloadCodec;
	}

	/**
	 * Configure the mandatory filter applied when payloads are deserialized.
	 * @param deserializationFilter the object input filter
	 * @since 7.2
	 */
	public void setDeserializationFilter(ObjectInputFilter deserializationFilter) {
		Assert.notNull(deserializationFilter, "'deserializationFilter' must not be null");
		this.deserializationFilter = deserializationFilter;
	}

	public final void setMessageStore(MessageGroupStore store) {
		this.messageStore = store;
		UniqueExpiryCallback expiryCallback =
				(messageGroupStore, group) -> this.forceReleaseProcessor.processMessageGroup(group);
		store.registerMessageGroupExpiryCallback(expiryCallback);
	}

	public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
		Assert.notNull(correlationStrategy, "'correlationStrategy' must not be null");
		this.correlationStrategy = correlationStrategy;
	}

	public void setReleaseStrategy(ReleaseStrategy releaseStrategy) {
		Assert.notNull(releaseStrategy, "'releaseStrategy' must not be null");
		this.releaseStrategy = releaseStrategy;
		this.sequenceAware = this.releaseStrategy instanceof SequenceSizeReleaseStrategy;
		this.releaseStrategySet = true;
	}

	public void setGroupTimeoutExpression(Expression groupTimeoutExpression) {
		this.groupTimeoutExpression = groupTimeoutExpression;
	}

	public void setForceReleaseAdviceChain(List<Advice> forceReleaseAdviceChain) {
		Assert.notNull(forceReleaseAdviceChain, "'forceReleaseAdviceChain' must not be null");
		this.forceReleaseAdviceChain = forceReleaseAdviceChain;
	}

	/**
	 * Specify a {@link MessageGroupProcessor} for the output function.
	 * @param outputProcessor the {@link MessageGroupProcessor} to use
	 * @since 5.0
	 */
	public void setOutputProcessor(MessageGroupProcessor outputProcessor) {
		Assert.notNull(outputProcessor, "'processor' must not be null");
		this.outputProcessor = outputProcessor;
	}

	/**
	 * Return a configured {@link MessageGroupProcessor}.
	 * @return the configured {@link MessageGroupProcessor}
	 * @since 5.2
	 */
	public MessageGroupProcessor getOutputProcessor() {
		return this.outputProcessor;
	}

	public void setDiscardChannel(MessageChannel discardChannel) {
		Assert.notNull(discardChannel, "'discardChannel' cannot be null");
		this.discardChannel = discardChannel;
	}

	public void setDiscardChannelName(String discardChannelName) {
		Assert.hasText(discardChannelName, "'discardChannelName' must not be empty");
		this.discardChannelName = discardChannelName;
	}

	public void setSendPartialResultOnExpiry(boolean sendPartialResultOnExpiry) {
		this.sendPartialResultOnExpiry = sendPartialResultOnExpiry;
	}

	/**
	 * Set to {@code false} to send to discard channel a whole expired group as a single message.
	 * This option makes sense only if {@link #sendPartialResultOnExpiry} is set to {@code false} (default).
	 * And also if {@link #discardChannel} is injected.
	 * @param discardIndividuallyOnExpiry false to discard the whole group as one message.
	 * @since 6.5
	 * @see #sendPartialResultOnExpiry
	 */
	public void setDiscardIndividuallyOnExpiry(boolean discardIndividuallyOnExpiry) {
		this.discardIndividuallyOnExpiry = discardIndividuallyOnExpiry;
	}

	/**
	 * By default, when a MessageGroupStoreReaper is configured to expire partial
	 * groups, empty groups are also removed. Empty groups exist after a group
	 * is released normally. This is to enable the detection and discarding of
	 * late-arriving messages. If you wish to expire empty groups on a longer
	 * schedule than expiring partial groups, set this property. Empty groups will
	 * then not be removed from the MessageStore until they have not been modified
	 * for at least this number of milliseconds.
	 * @param minimumTimeoutForEmptyGroups The minimum timeout.
	 */
	public void setMinimumTimeoutForEmptyGroups(long minimumTimeoutForEmptyGroups) {
		this.minimumTimeoutForEmptyGroups = minimumTimeoutForEmptyGroups;
	}

	/**
	 * Set {@code releasePartialSequences} on an underlying default
	 * {@link SequenceSizeReleaseStrategy}. Ignored for other release strategies.
	 * @param releasePartialSequences true to allow release.
	 */
	public void setReleasePartialSequences(boolean releasePartialSequences) {
		if (!this.releaseStrategySet && releasePartialSequences) {
			setReleaseStrategy(new SequenceSizeReleaseStrategy(releasePartialSequences));
		}
		this.releasePartialSequences = releasePartialSequences;
	}

	/**
	 * Expire (completely remove) a group if it is completed due to timeout.
	 * Default is {@code true}.
	 * @param expireGroupsUponTimeout the expireGroupsUponTimeout to set
	 * @since 4.1
	 */
	public void setExpireGroupsUponTimeout(boolean expireGroupsUponTimeout) {
		this.expireGroupsUponTimeout = expireGroupsUponTimeout;
	}

	/**
	 * Perform a
	 * {@link org.springframework.integration.support.MessageBuilder#popSequenceDetails()}
	 * for output message or not. Default is {@code true}. This option removes the sequence
	 * information added by the nearest upstream component with {@code applySequence=true}
	 * (for example splitter).
	 * @param popSequence the boolean flag to use.
	 * @since 5.1
	 */
	public void setPopSequence(boolean popSequence) {
		this.popSequence = popSequence;
	}

	protected boolean isReleaseLockBeforeSend() {
		return this.releaseLockBeforeSend;
	}

	/**
	 * Set to true to release the message group lock before sending any output. See
	 * "Avoiding Deadlocks" in the Aggregator section of the reference manual for more
	 * information as to why this might be needed.
	 * @param releaseLockBeforeSend true to release the lock.
	 * @since 5.1.1
	 */
	public void setReleaseLockBeforeSend(boolean releaseLockBeforeSend) {
		this.releaseLockBeforeSend = releaseLockBeforeSend;
	}

	/**
	 * Configure a timeout in milliseconds for purging old orphaned groups from the store.
	 * Used on startup and when an {@link #expireDuration} is provided, the task for running
	 * {@link #purgeOrphanedGroups()} is scheduled with that period.
	 * The {@link #forceReleaseProcessor} is used to process those expired groups according
	 * the "force complete" options. A group can be orphaned if a persistent message group
	 * store is used and no new messages arrive for that group after a restart.
	 * @param expireTimeout the number of milliseconds to determine old orphaned groups in the store to purge.
	 * @since 5.4
	 * @see #purgeOrphanedGroups()
	 */
	public void setExpireTimeout(long expireTimeout) {
		Assert.isTrue(expireTimeout > 0, "'expireTimeout' must be more than 0.");
		this.expireTimeout = expireTimeout;
	}

	/**
	 * Configure a {@link Duration} (in millis) how often to clean up old orphaned groups from the store.
	 * @param expireDuration the delay how often to call {@link #purgeOrphanedGroups()}.
	 * @since 5.4
	 * @see #purgeOrphanedGroups()
	 * @see #setExpireDuration(Duration)
	 * @see #setExpireTimeout(long)
	 */
	public void setExpireDurationMillis(long expireDuration) {
		setExpireDuration(Duration.ofMillis(expireDuration));
	}

	/**
	 * Configure a {@link Duration} how often to clean up old orphaned groups from the store.
	 * @param expireDuration the delay how often to call {@link #purgeOrphanedGroups()}.
	 * @since 5.4
	 * @see #purgeOrphanedGroups()
	 * @see #setExpireTimeout(long)
	 */
	public void setExpireDuration(@Nullable Duration expireDuration) {
		this.expireDuration = expireDuration;
	}

	/**
	 * Configure a {@link BiFunction} to supply a group condition from a message to be added to the group.
	 * The {@code null} result from the function will reset a condition set before.
	 * @param conditionSupplier the function to supply a group condition from a message to be added to the group.
	 * @since 5.5
	 * @see GroupConditionProvider
	 */
	public void setGroupConditionSupplier(BiFunction<Message<?>, String, String> conditionSupplier) {
		this.groupConditionSupplier = conditionSupplier;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.state(!(this.discardChannelName != null && this.discardChannel != null),
				"'discardChannelName' and 'discardChannel' are mutually exclusive.");
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			if (this.outputProcessor instanceof BeanFactoryAware beanFactoryAware) {
				beanFactoryAware.setBeanFactory(beanFactory);
			}
			if (this.correlationStrategy instanceof BeanFactoryAware beanFactoryAware) {
				beanFactoryAware.setBeanFactory(beanFactory);
			}
			if (this.releaseStrategy instanceof BeanFactoryAware beanFactoryAware) {
				beanFactoryAware.setBeanFactory(beanFactory);
			}
		}

		if (this.releasePartialSequences) {
			Assert.isInstanceOf(SequenceSizeReleaseStrategy.class, this.releaseStrategy, () ->
					"Release strategy of type [" + this.releaseStrategy.getClass().getSimpleName() +
							"] cannot release partial sequences. Use a SequenceSizeReleaseStrategy instead.");
			((SequenceSizeReleaseStrategy) this.releaseStrategy)
					.setReleasePartialSequences(this.releasePartialSequences);
		}

		if (this.evaluationContext == null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		}

		if (this.sequenceAware) {
			this.logger.warn("Using a SequenceSizeReleaseStrategy with large groups may not perform well, consider "
					+ "using a SimpleSequenceSizeReleaseStrategy");
		}

		/*
		 * Disallow any further changes to the lock registry
		 * (checked in the setter).
		 */
		this.lockRegistrySet = true;
		this.forceReleaseProcessor = createGroupTimeoutProcessor();

		if (this.releaseStrategy instanceof GroupConditionProvider groupConditionProvider) {
			this.groupConditionSupplier = groupConditionProvider.getGroupConditionSupplier();
		}

		initializeCorrelatingAgent();
	}

	private synchronized void initializeCorrelatingAgent() {
		if (this.correlatingAgent != null) {
			return;
		}
		CorrelatingDependencyGateway dependencyGateway = new CorrelatingDependencyGateway();
		this.correlatingDependencyGateway = dependencyGateway;
		Channel channel = this.correlatingAgentChannel;
		if (channel == null) {
			String serverName = InProcessServerBuilder.generateName();
			ManagedChannel managedChannel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
			try {
				this.correlatingAgentServer = InProcessServerBuilder.forName(serverName)
						.directExecutor()
						.addService(dependencyGateway)
						.addService(new EmbabelCorrelatingAgentService(managedChannel))
						.build()
						.start();
			}
			catch (IOException ex) {
				managedChannel.shutdownNow();
				throw new IllegalStateException("Failed to start the in-process correlating agent", ex);
			}
			this.managedCorrelatingAgentChannel = managedChannel;
			channel = managedChannel;
		}
		this.correlatingAgent = CorrelatingAgentPortGrpc.newBlockingStub(channel);
	}

	private MessageGroupProcessor createGroupTimeoutProcessor() {
		MessageGroupProcessor processor = new ForceReleaseMessageGroupProcessor();

		if (this.groupTimeoutExpression != null && !CollectionUtils.isEmpty(this.forceReleaseAdviceChain)) {
			ProxyFactory proxyFactory = new ProxyFactory(processor);
			this.forceReleaseAdviceChain.forEach(proxyFactory::addAdvice);
			return (MessageGroupProcessor) proxyFactory.getProxy(getApplicationContext().getClassLoader());
		}
		return processor;
	}

	@Override
	public String getComponentType() {
		return "aggregator";
	}

	public MessageGroupStore getMessageStore() {
		return this.messageStore;
	}

	protected Map<UUID, ScheduledFuture<?>> getExpireGroupScheduledFutures() {
		return this.expireGroupScheduledFutures;
	}

	protected CorrelationStrategy getCorrelationStrategy() {
		return this.correlationStrategy;
	}

	protected ReleaseStrategy getReleaseStrategy() {
		return this.releaseStrategy;
	}

	@Nullable
	protected BiFunction<Message<?>, String, String> getGroupConditionSupplier() {
		return this.groupConditionSupplier;
	}

	@Override
	@Nullable
	public  MessageChannel getDiscardChannel() {
		String channelName = this.discardChannelName;
		if (channelName == null && this.discardChannel == null) {
			channelName = IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME;
		}
		if (channelName != null) {
			try {
				this.discardChannel = getChannelResolver().resolveDestination(channelName);
			}
			catch (DestinationResolutionException ex) {
				if (channelName.equals(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME)) {
					this.discardChannel = new NullChannel();
				}
				else {
					throw ex;
				}
			}
			this.discardChannelName = null;
		}
		return this.discardChannel;
	}

	@Nullable
	protected String getDiscardChannelName() {
		return this.discardChannelName;
	}

	protected boolean isSendPartialResultOnExpiry() {
		return this.sendPartialResultOnExpiry;
	}

	protected boolean isSequenceAware() {
		return this.sequenceAware;
	}

	protected LockRegistry<?> getLockRegistry() {
		return this.lockRegistry;
	}

	protected boolean isLockRegistrySet() {
		return this.lockRegistrySet;
	}

	protected long getMinimumTimeoutForEmptyGroups() {
		return this.minimumTimeoutForEmptyGroups;
	}

	protected boolean isReleasePartialSequences() {
		return this.releasePartialSequences;
	}

	@Nullable
	protected Expression getGroupTimeoutExpression() {
		return this.groupTimeoutExpression;
	}

	protected EvaluationContext getEvaluationContext() {
		return this.evaluationContext;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		String invocationId = UUID.randomUUID().toString();
		CorrelatingAgentPortGrpc.CorrelatingAgentPortBlockingStub agent = obtainCorrelatingAgent();
		CorrelatingDependencyGateway dependencyGateway = this.correlatingDependencyGateway;
		if (dependencyGateway != null) {
			dependencyGateway.register(invocationId, message);
		}
		try {
			MessageEnvelope envelope = CorrelatingMessageMapper.toEnvelope(message, this.payloadCodec);
			HandleMessageResponse response = agent
					.withDeadlineAfter(this.correlatingAgentDeadline.toNanos(), TimeUnit.NANOSECONDS)
					.handleMessage(HandleMessageRequest.newBuilder()
							.setInvocationId(invocationId)
							.setMessage(envelope)
							.build());
			Assert.state(response.getOutcome() != DecisionOutcome.DECISION_OUTCOME_UNSPECIFIED
					&& response.getOutcome() != DecisionOutcome.STALE,
					() -> "Correlating agent returned an invalid outcome: " + response.getOutcome());
		}
		catch (StatusRuntimeException ex) {
			String description = ex.getStatus().getDescription();
			RuntimeException cause;
			if (description != null && description.startsWith("DEPENDENCY_FAILURE: ")) {
				String dependencyMessage = description.substring("DEPENDENCY_FAILURE: ".length());
				cause = dependencyMessage.contains("Null correlation")
						? new IllegalStateException(dependencyMessage)
						: new RuntimeException(dependencyMessage);
			}
			else {
				cause = ex;
			}
			throw new MessageHandlingException(message, "Correlating agent invocation failed in [" + this + ']', cause);
		}
		catch (RuntimeException ex) {
			throw new MessageHandlingException(message, "Correlating agent invocation failed in [" + this + ']', ex);
		}
		finally {
			if (dependencyGateway != null) {
				dependencyGateway.unregister(invocationId);
			}
		}
	}

	private CorrelatingAgentPortGrpc.CorrelatingAgentPortBlockingStub obtainCorrelatingAgent() {
		CorrelatingAgentPortGrpc.CorrelatingAgentPortBlockingStub agent = this.correlatingAgent;
		if (agent == null) {
			initializeCorrelatingAgent();
			agent = this.correlatingAgent;
		}
		Assert.state(agent != null, "The correlating agent could not be initialized");
		return agent;
	}

	private void cancelScheduledFutureIfAny(Object correlationKey, UUID groupIdUuid, boolean mayInterruptIfRunning) {
		ScheduledFuture<?> scheduledFuture = this.expireGroupScheduledFutures.remove(groupIdUuid);
		if (scheduledFuture != null) {
			boolean canceled = scheduledFuture.cancel(mayInterruptIfRunning);
			if (canceled) {
				this.logger.debug(() ->
						"Cancel 'ScheduledFuture' for MessageGroup with Correlation Key [ " + correlationKey + "].");
			}
		}
	}

	protected boolean isExpireGroupsUponCompletion() {
		return false;
	}

	private void removeEmptyGroupAfterTimeout(UUID groupId, long timeout) {
		ScheduledFuture<?> scheduledFuture =
				getTaskScheduler()
						.schedule(() -> {
							Lock lock = this.lockRegistry.obtain(groupId.toString());

							try {
								lock.lockInterruptibly();
								try {
									this.expireGroupScheduledFutures.remove(groupId);
									/*
									 * Obtain a fresh state for group from the MessageStore,
									 * since it could be changed while we have waited for lock.
									 */
									MessageGroup groupNow = this.messageStore.getMessageGroup(groupId);
									boolean removeGroup = groupNow.size() == 0 &&
											groupNow.getLastModified()
													<= (System.currentTimeMillis() - this.minimumTimeoutForEmptyGroups);
									if (removeGroup) {
										this.logger.debug(() -> "Removing empty group: " + groupId);
										remove(groupNow);
									}
								}
								finally {
									lock.unlock();
								}
							}
							catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								this.logger.debug(() -> "Thread was interrupted while trying to obtain lock."
										+ "Rescheduling empty MessageGroup [ " + groupId + "] for removal.");
								removeEmptyGroupAfterTimeout(groupId, timeout);
							}

						}, Instant.now().plusMillis(timeout));

		this.logger.debug(() -> "Schedule empty MessageGroup [ " + groupId + "] for removal.");
		this.expireGroupScheduledFutures.put(groupId, scheduledFuture);
	}

	private void scheduleGroupToForceComplete(MessageGroup messageGroup) {
		Object groupTimeout = obtainGroupTimeout(messageGroup);
		/*
		 * When 'groupTimeout' is evaluated to 'null' we do nothing.
		 * The 'MessageGroupStoreReaper' can be used to 'forceComplete' message groups.
		 */
		if (groupTimeout != null) {
			Date startTime = null;
			if (groupTimeout instanceof Date date) {
				startTime = date;
			}
			else if ((Long) groupTimeout > 0) {
				startTime = new Date(System.currentTimeMillis() + (Long) groupTimeout);
			}

			if (startTime != null) {
				Object groupId = messageGroup.getGroupId();
				long timestamp = messageGroup.getTimestamp();
				long lastModified = messageGroup.getLastModified();
				ScheduledFuture<?> scheduledFuture =
						getTaskScheduler()
								.schedule(() -> {
									try {
										processForceRelease(groupId, timestamp, lastModified);
									}
									catch (MessageDeliveryException ex) {
										logger.warn(ex, () ->
												"The MessageGroup [" + groupId +
														"] is rescheduled by the reason of: ");
										scheduleGroupToForceComplete(groupId);
									}
								}, startTime.toInstant());

				this.logger.debug(() -> "Schedule MessageGroup [ " + messageGroup + "] to 'forceComplete'.");
				this.expireGroupScheduledFutures.put(UUIDConverter.getUUID(groupId), scheduledFuture);
			}
			else {
				this.forceReleaseProcessor.processMessageGroup(messageGroup);
			}
		}
	}

	private void scheduleGroupToForceComplete(Object groupId) {
		MessageGroup messageGroup = this.messageStore.getMessageGroup(groupId);
		scheduleGroupToForceComplete(messageGroup);
	}

	private void processForceRelease(Object groupId, long timestamp, long lastModified) {
		MessageGroup messageGroup = this.messageStore.getMessageGroup(groupId);
		if (messageGroup.getTimestamp() == timestamp && messageGroup.getLastModified() == lastModified) {
			this.forceReleaseProcessor.processMessageGroup(messageGroup);
		}
	}

	private void discardMessage(Message<?> message) {
		MessageChannel messageChannel = getDiscardChannel();
		if (messageChannel != null) {
			this.messagingTemplate.send(messageChannel, message);
		}
	}

	/**
	 * Allows you to provide additional logic that needs to be performed after the MessageGroup was released.
	 * @param group The group.
	 * @param completedMessages The completed messages.
	 */
	protected abstract void afterRelease(MessageGroup group, @Nullable Collection<Message<?>> completedMessages);

	/**
	 * Subclasses may override if special action is needed because the group was released or discarded
	 * due to a timeout. By default, {@link #afterRelease(MessageGroup, Collection)} is invoked.
	 * @param group The group.
	 * @param completedMessages The completed messages.
	 * @param timeout True if the release/discard was due to a timeout.
	 */
	protected void afterRelease(MessageGroup group, Collection<Message<?>> completedMessages, boolean timeout) {
		afterRelease(group, completedMessages);
	}

	protected void forceComplete(MessageGroup group) {
		String invocationId = UUID.randomUUID().toString();
		CorrelatingAgentPortGrpc.CorrelatingAgentPortBlockingStub agent = obtainCorrelatingAgent();
		CorrelatingDependencyGateway dependencyGateway = this.correlatingDependencyGateway;
		if (dependencyGateway != null) {
			dependencyGateway.register(invocationId, group);
		}
		try {
			ForceCompleteResponse response = agent
					.withDeadlineAfter(this.correlatingAgentDeadline.toNanos(), TimeUnit.NANOSECONDS)
					.forceComplete(ForceCompleteRequest.newBuilder()
							.setInvocationId(invocationId)
							.setGroupId(this.payloadCodec.encode(group.getGroupId()))
							.setCandidateTimestamp(group.getTimestamp())
							.setCandidateLastModified(group.getLastModified())
							.build());
			Assert.state(response.getOutcome() != DecisionOutcome.DECISION_OUTCOME_UNSPECIFIED
					&& response.getOutcome() != DecisionOutcome.STALE,
					() -> "Correlating agent returned an invalid force-complete outcome: " + response.getOutcome());
		}
		catch (StatusRuntimeException ex) {
			String description = ex.getStatus().getDescription();
			if (description != null && description.startsWith("MESSAGE_DELIVERY: ")) {
				Message<?> failedMessage = Objects.requireNonNull(group.getOne(), "The expiring group is empty");
				throw new MessageDeliveryException(failedMessage,
						description.substring("MESSAGE_DELIVERY: ".length()), ex);
			}
			throw new IllegalStateException("Correlating agent force-complete invocation failed in [" + this + ']', ex);
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Correlating agent force-complete invocation failed in [" + this + ']', ex);
		}
		finally {
			if (dependencyGateway != null) {
				dependencyGateway.unregister(invocationId);
			}
		}
	}

	protected void remove(MessageGroup group) {
		Object correlationKey = group.getGroupId();
		this.messageStore.removeMessageGroup(correlationKey);
	}

	protected int findLastReleasedSequenceNumber(@SuppressWarnings("unused") Object groupId,
			Collection<Message<?>> partialSequence) {

		Message<?> lastReleasedMessage = Collections.max(partialSequence, this.sequenceNumberComparator);
		return StaticMessageHeaderAccessor.getSequenceNumber(lastReleasedMessage);
	}

	protected MessageGroup store(Object correlationKey, Message<?> message) {
		return this.messageStore.addMessageToGroup(correlationKey, message);
	}

	protected void expireGroup(Object correlationKey, MessageGroup group, Lock lock) {
		this.logger.debug(() -> "Expiring MessageGroup with correlationKey[" + correlationKey + "]");
		if (this.sendPartialResultOnExpiry) {
			this.logger.debug(() -> "Prematurely releasing partially complete group with key ["
					+ correlationKey + "] to: " + getOutputChannel());
			completeGroup(correlationKey, group, lock);
		}
		else {
			this.logger.debug(() -> "Discarding messages of partially complete group with key ["
					+ correlationKey + "] to: "
					+ (this.discardChannelName != null ? this.discardChannelName : this.discardChannel));
			if (this.releaseLockBeforeSend) {
				lock.unlock();
			}
			MessageChannel messageChannel = getDiscardChannel();
			if (messageChannel != null) {
				if (this.discardIndividuallyOnExpiry) {
					group.getMessages()
							.forEach(this::discardMessage);
				}
				else {
					List<Message<?>> messagesInGroupToDiscard = new ArrayList<>(group.getMessages());
					discardMessage(new GenericMessage<>(messagesInGroupToDiscard));
				}
			}
		}
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(
					new MessageGroupExpiredEvent(this, correlationKey, group.size(),
							new Date(group.getLastModified()), new Date(), !this.sendPartialResultOnExpiry));
		}
	}

	@SuppressWarnings("NullAway") // Never called with an empty group
	protected void completeGroup(Object correlationKey, MessageGroup group, Lock lock) {
		completeGroup(group.getOne(), correlationKey, group, lock);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	protected Collection<Message<?>> completeGroup(Message<?> message, Object correlationKey, MessageGroup group,
			Lock lock) {

		Collection<Message<?>> partialSequence = null;
		Object result;
		try {
			this.logger.debug(() -> "Completing group with correlationKey [" + correlationKey + "]");

			result = this.outputProcessor.processMessageGroup(group);
			Assert.state(result != null, "The processorMessageGroup returned a null result. Null result is not expected.");
			if (isResultCollectionOfMessages(result)) {
				partialSequence = (Collection<Message<?>>) result;
			}

			if (this.popSequence && partialSequence == null) {
				AbstractIntegrationMessageBuilder<?> messageBuilder = null;
				if (result instanceof AbstractIntegrationMessageBuilder<?>) {
					messageBuilder = (AbstractIntegrationMessageBuilder<?>) result;
				}
				else if (!(result instanceof Message<?>)) {
					messageBuilder =
							getMessageBuilderFactory()
									.withPayload(result)
									.copyHeaders(message.getHeaders());
				}
				else if (compareSequences((Message<?>) result, message)) {
					messageBuilder =
							getMessageBuilderFactory()
									.fromMessage((Message<?>) result);
				}
				result = messageBuilder != null ? messageBuilder.popSequenceDetails() : result;
			}
		}
		finally {
			if (this.releaseLockBeforeSend) {
				lock.unlock();
			}
		}
		sendOutputs(result, message);
		return partialSequence;
	}

	private static boolean compareSequences(Message<?> msg1, Message<?> msg2) {
		Object sequence1 = msg1.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS);
		Object sequence2 = msg2.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS);
		return ObjectUtils.nullSafeEquals(sequence1, sequence2);

	}

	private static boolean isResultCollectionOfMessages(Object result) {
		if (result instanceof Collection<?> resultCollection) {
			Class<?> commonElementType = CollectionUtils.findCommonElementType(resultCollection);
			return commonElementType != null && Message.class.isAssignableFrom(commonElementType);
		}
		return false;
	}

	@Nullable
	protected Object obtainGroupTimeout(MessageGroup group) {
		if (this.groupTimeoutExpression != null) {
			Object timeout = this.groupTimeoutExpression.getValue(this.evaluationContext, group);
			if (timeout instanceof Date) {
				return timeout;
			}
			else if (timeout != null) {
				try {
					return Long.parseLong(timeout.toString());
				}
				catch (NumberFormatException ex) {
					throw new IllegalStateException("Error evaluating 'groupTimeoutExpression'", ex);
				}
			}
		}
		return null;
	}

	@Override
	public void destroy() {
		this.expireGroupScheduledFutures.values().forEach(future -> future.cancel(true));
		Server server = this.correlatingAgentServer;
		if (server != null) {
			server.shutdownNow();
		}
		ManagedChannel channel = this.managedCorrelatingAgentChannel;
		if (channel != null) {
			channel.shutdownNow();
		}
	}

	@Override
	public void start() {
		if (!this.running) {
			CorrelatingAgentPortGrpc.CorrelatingAgentPortBlockingStub agent = this.correlatingAgent;
			if (agent != null) {
				agent.withDeadlineAfter(this.correlatingAgentDeadline.toNanos(), TimeUnit.NANOSECONDS)
						.start(LifecycleRequest.getDefaultInstance());
			}
			this.running = true;
			if (this.outputProcessor instanceof Lifecycle lifecycle) {
				lifecycle.start();
			}
			if (this.releaseStrategy instanceof Lifecycle lifecycle) {
				lifecycle.start();
			}
			if (this.expireTimeout > 0) {
				purgeOrphanedGroups();
				if (this.expireDuration != null) {
					getTaskScheduler()
							.scheduleWithFixedDelay(this::purgeOrphanedGroups, this.expireDuration);
				}
			}
		}
	}

	@Override
	public void stop() {
		if (this.running) {
			CorrelatingAgentPortGrpc.CorrelatingAgentPortBlockingStub agent = this.correlatingAgent;
			if (agent != null) {
				agent.withDeadlineAfter(this.correlatingAgentDeadline.toNanos(), TimeUnit.NANOSECONDS)
						.stop(LifecycleRequest.getDefaultInstance());
			}
			this.running = false;
			if (this.outputProcessor instanceof Lifecycle lifecycle) {
				lifecycle.stop();
			}
			if (this.releaseStrategy instanceof Lifecycle lifecycle) {
				lifecycle.stop();
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Perform a {@link MessageGroupStore#expireMessageGroups(long)} with the provided {@link #expireTimeout}.
	 * Can be called externally at any time.
	 * Internally it is called from the scheduled task with the configured {@link #expireDuration}.
	 * @since 5.4
	 */
	public void purgeOrphanedGroups() {
		Assert.isTrue(this.expireTimeout > 0, "'expireTimeout' must be more than 0.");
		this.messageStore.expireMessageGroups(this.expireTimeout);
	}

	private final class CorrelatingDependencyGateway
			extends CorrelatingDependencyPortGrpc.CorrelatingDependencyPortImplBase {

		private final Map<String, Message<?>> localMessages = new ConcurrentHashMap<>();

		private final Map<String, MessageAssessment> assessments = new ConcurrentHashMap<>();

		private final Map<String, OperationContext> operations = new ConcurrentHashMap<>();

		private final Map<String, MessageGroup> forceGroups = new ConcurrentHashMap<>();

		void register(String invocationId, Message<?> message) {
			this.localMessages.put(invocationId, message);
		}

		void register(String invocationId, MessageGroup group) {
			this.forceGroups.put(invocationId, group);
		}

		void unregister(String invocationId) {
			this.localMessages.remove(invocationId);
			this.assessments.remove(invocationId);
			this.operations.remove(invocationId);
			this.forceGroups.remove(invocationId);
		}

		@Override
		public void evaluateMessage(EvaluateMessageRequest request,
				StreamObserver<MessageAssessment> responseObserver) {

			try {
				OperationContext operation = obtainOperation(request.getInvocationId(), request.getMessage());
				Message<?> message = operation.message();
				Object correlationKey = operation.correlationKey();
				Lock lock = obtainGroupLock(correlationKey);
				lock.lockInterruptibly();
				try {
					MessageGroup storedGroup = AbstractCorrelatingMessageHandler.this.messageStore
							.getMessageGroup(correlationKey);
					MessageGroup group = AbstractCorrelatingMessageHandler.this.sequenceAware
							? new SequenceAwareMessageGroup(storedGroup) : storedGroup;
					boolean messagePresent = containsMessage(group, message);
					boolean canAdd = !group.isComplete() && (messagePresent || group.canAdd(message));
					ConditionAssessment condition = assessCondition(message, group, messagePresent);
					boolean canRelease = false;
					if (canAdd) {
						MessageGroup candidate =
								createCandidateGroup(storedGroup, message, messagePresent, condition.value());
						canRelease = AbstractCorrelatingMessageHandler.this.releaseStrategy.canRelease(candidate);
					}
					MessageAssessment assessment = MessageAssessment.newBuilder()
							.setInvocationId(request.getInvocationId())
							.setVersion(groupVersion(storedGroup))
							.setGroupComplete(group.isComplete())
							.setCanAdd(canAdd)
							.setCanReleaseAfterStore(canRelease)
							.setMessagePresent(messagePresent)
							.setConditionEvaluated(condition.evaluated())
							.setConditionPresent(condition.value() != null)
							.setCondition(condition.value() != null ? condition.value() : "")
							.build();
					this.assessments.put(request.getInvocationId(), assessment);
					responseObserver.onNext(assessment);
					responseObserver.onCompleted();
				}
				finally {
					lock.unlock();
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				responseObserver.onError(Status.CANCELLED.withDescription("Interrupted obtaining the group lock")
						.withCause(ex).asRuntimeException());
			}
			catch (RuntimeException ex) {
				responseObserver.onError(toStatusException(ex));
			}
		}

		@Override
		@SuppressWarnings("NullAway") // MessageGroupStore accepts null to reset a group condition.
		public void applyMessageDecision(ApplyMessageDecisionRequest request,
				StreamObserver<ApplyDecisionResponse> responseObserver) {

			boolean lockHeld = false;
			Lock lock = null;
			try {
				MessageAssessment assessment = this.assessments.get(request.getInvocationId());
				validateMessageDecision(request.getDecision(), assessment);
				OperationContext operation = obtainOperation(request.getInvocationId(), request.getMessage());
				Message<?> message = operation.message();
				Object correlationKey = operation.correlationKey();
				UUID groupId = UUIDConverter.getUUID(correlationKey);
				lock = AbstractCorrelatingMessageHandler.this.lockRegistry.obtain(groupId.toString());
				lock.lockInterruptibly();
				lockHeld = true;
				MessageGroup storedGroup = AbstractCorrelatingMessageHandler.this.messageStore
						.getMessageGroup(correlationKey);
				long currentVersion = groupVersion(storedGroup);
				if (currentVersion != request.getExpectedVersion()) {
					respond(responseObserver, DecisionOutcome.STALE, currentVersion, "Group version changed");
					return;
				}

				MessageGroup group = AbstractCorrelatingMessageHandler.this.sequenceAware
						? new SequenceAwareMessageGroup(storedGroup) : storedGroup;
				boolean messagePresent = containsMessage(group, message);
				if (request.getDecision() == MessageDecision.DISCARD) {
					if (AbstractCorrelatingMessageHandler.this.releaseLockBeforeSend) {
						lock.unlock();
						lockHeld = false;
					}
					discardMessage(this.localMessages.getOrDefault(request.getInvocationId(), message));
					respond(responseObserver, DecisionOutcome.DISCARDED, currentVersion, "Message discarded");
					return;
				}

				if (group.isComplete() || (!messagePresent && !group.canAdd(message))) {
					respond(responseObserver, DecisionOutcome.STALE, currentVersion, "Group no longer accepts message");
					return;
				}
				cancelScheduledFutureIfAny(correlationKey, groupId, true);
				if (!messagePresent) {
					group = store(correlationKey, message);
					if (request.getConditionEvaluated()) {
						AbstractCorrelatingMessageHandler.this.messageStore.setGroupCondition(group.getGroupId(),
								request.getConditionPresent() ? request.getCondition() : null);
						group = AbstractCorrelatingMessageHandler.this.messageStore.getMessageGroup(group.getGroupId());
					}
					if (AbstractCorrelatingMessageHandler.this.sequenceAware) {
						group = new SequenceAwareMessageGroup(group);
					}
				}

				if (request.getDecision() == MessageDecision.STORE_AND_RELEASE) {
					Collection<Message<?>> completedMessages = null;
					if (AbstractCorrelatingMessageHandler.this.releaseLockBeforeSend) {
						lockHeld = false;
					}
					try {
						completedMessages = completeGroup(message, correlationKey, group, lock);
					}
					finally {
						afterRelease(group, completedMessages);
					}
					if (!isExpireGroupsUponCompletion()
							&& AbstractCorrelatingMessageHandler.this.minimumTimeoutForEmptyGroups > 0) {

						removeEmptyGroupAfterTimeout(groupId,
								AbstractCorrelatingMessageHandler.this.minimumTimeoutForEmptyGroups);
					}
					respond(responseObserver, DecisionOutcome.RELEASED, groupVersion(group), "Group released");
				}
				else if (request.getDecision() == MessageDecision.STORE_AND_WAIT) {
					scheduleGroupToForceComplete(group);
					respond(responseObserver, DecisionOutcome.WAITING, groupVersion(group), "Message stored");
				}
				else {
					throw new IllegalArgumentException("Unsupported message decision: " + request.getDecision());
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				responseObserver.onError(Status.CANCELLED.withDescription("Interrupted obtaining the group lock")
						.withCause(ex).asRuntimeException());
			}
			catch (RuntimeException ex) {
				responseObserver.onError(toStatusException(ex));
			}
			finally {
				if (lockHeld && lock != null) {
					lock.unlock();
				}
			}
		}

		@Override
		public void evaluateForceComplete(EvaluateForceCompleteRequest request,
				StreamObserver<ForceCompleteAssessment> responseObserver) {

			try {
				Object correlationKey = decodeGroupId(request.getGroupId());
				Lock lock = obtainGroupLock(correlationKey);
				lock.lockInterruptibly();
				try {
					MessageGroup group = this.forceGroups.get(request.getInvocationId());
					if (group == null || !group.isComplete()) {
						group = AbstractCorrelatingMessageHandler.this.messageStore.getMessageGroup(correlationKey);
					}
					this.forceGroups.put(request.getInvocationId(), group);
					int size = group.size();
					boolean unchanged = request.getCandidateTimestamp() == group.getTimestamp()
							&& request.getCandidateLastModified() == group.getLastModified();
					boolean eligible = unchanged && (!group.isComplete() || size == 0);
					if (eligible && size == 0) {
						eligible = group.getLastModified()
								<= (System.currentTimeMillis()
										- AbstractCorrelatingMessageHandler.this.minimumTimeoutForEmptyGroups);
					}
					ForceCompleteAssessment assessment = ForceCompleteAssessment.newBuilder()
							.setInvocationId(request.getInvocationId())
							.setVersion(groupVersion(group))
							.setEligible(eligible)
							.setEmpty(size == 0)
							.setCanRelease(eligible && size > 0
									&& AbstractCorrelatingMessageHandler.this.releaseStrategy.canRelease(group))
							.build();
					responseObserver.onNext(assessment);
					responseObserver.onCompleted();
				}
				finally {
					lock.unlock();
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				responseObserver.onError(Status.CANCELLED.withDescription("Interrupted obtaining the group lock")
						.withCause(ex).asRuntimeException());
			}
			catch (RuntimeException ex) {
				responseObserver.onError(toStatusException(ex));
			}
		}

		@Override
		public void applyForceCompleteDecision(ApplyForceCompleteDecisionRequest request,
				StreamObserver<ApplyDecisionResponse> responseObserver) {

			Lock lock = null;
			boolean lockHeld = false;
			boolean removeGroup = false;
			MessageGroup group = null;
			try {
				Object correlationKey = decodeGroupId(request.getGroupId());
				UUID groupId = UUIDConverter.getUUID(correlationKey);
				lock = AbstractCorrelatingMessageHandler.this.lockRegistry.obtain(groupId.toString());
				lock.lockInterruptibly();
				lockHeld = true;
				group = this.forceGroups.get(request.getInvocationId());
				if (group == null) {
					group = AbstractCorrelatingMessageHandler.this.messageStore.getMessageGroup(correlationKey);
				}
				long currentVersion = groupVersion(group);
				if (currentVersion != request.getExpectedVersion()) {
					respond(responseObserver, DecisionOutcome.STALE, currentVersion, "Group version changed");
					return;
				}
				cancelScheduledFutureIfAny(correlationKey, groupId, false);
				ForceCompleteDecision decision = request.getDecision();
				if (decision == ForceCompleteDecision.IGNORE) {
					respond(responseObserver, DecisionOutcome.IGNORED, currentVersion, "Group is not eligible");
					return;
				}
				if (decision == ForceCompleteDecision.REMOVE_EMPTY) {
					removeGroup = true;
					respond(responseObserver, DecisionOutcome.REMOVED, currentVersion, "Empty group removed");
					return;
				}
				if (decision == ForceCompleteDecision.RELEASE) {
					if (AbstractCorrelatingMessageHandler.this.releaseLockBeforeSend) {
						lockHeld = false;
					}
					completeGroup(correlationKey, group, lock);
				}
				else if (decision == ForceCompleteDecision.EXPIRE) {
					if (AbstractCorrelatingMessageHandler.this.releaseLockBeforeSend) {
						lockHeld = false;
					}
					expireGroup(correlationKey, group, lock);
				}
				else {
					throw new IllegalArgumentException("Unsupported force-complete decision: " + decision);
				}
				if (!AbstractCorrelatingMessageHandler.this.expireGroupsUponTimeout) {
					afterRelease(group, group.getMessages(), true);
				}
				else {
					removeGroup = true;
				}
				respond(responseObserver,
						decision == ForceCompleteDecision.RELEASE ? DecisionOutcome.RELEASED : DecisionOutcome.EXPIRED,
						currentVersion, "Group force-completed");
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				responseObserver.onError(Status.CANCELLED.withDescription("Interrupted obtaining the group lock")
						.withCause(ex).asRuntimeException());
			}
			catch (MessageDeliveryException ex) {
				removeGroup = false;
				responseObserver.onError(toStatusException(ex));
			}
			catch (RuntimeException ex) {
				responseObserver.onError(toStatusException(ex));
			}
			finally {
				try {
					if (removeGroup && group != null) {
						remove(group);
					}
				}
				finally {
					if (lockHeld && lock != null) {
						lock.unlock();
					}
				}
			}
		}

		private Message<?> decodeMessage(String invocationId, MessageEnvelope envelope) {
			Message<?> localMessage = this.localMessages.get(invocationId);
			Map<String, Object> localHeaders = localMessage != null
					? new HashMap<>(localMessage.getHeaders())
					: Collections.emptyMap();
			return CorrelatingMessageMapper.fromEnvelope(envelope,
					AbstractCorrelatingMessageHandler.this.payloadCodec,
					AbstractCorrelatingMessageHandler.this.deserializationFilter, localHeaders);
		}

		private OperationContext obtainOperation(String invocationId, MessageEnvelope envelope) {
			OperationContext operation = this.operations.get(invocationId);
			if (operation == null) {
				Message<?> message = decodeMessage(invocationId, envelope);
				operation = new OperationContext(message, resolveCorrelationKey(message));
				this.operations.put(invocationId, operation);
			}
			return operation;
		}

		private Object decodeGroupId(org.springframework.integration.aggregator.agent.grpc.SerializedObject groupId) {
			Object result = AbstractCorrelatingMessageHandler.this.payloadCodec.decode(groupId,
					AbstractCorrelatingMessageHandler.this.deserializationFilter);
			Assert.state(result != null, "Correlating group id cannot be null");
			return result;
		}

		private Object resolveCorrelationKey(Message<?> message) {
			Object correlationKey = AbstractCorrelatingMessageHandler.this.correlationStrategy
					.getCorrelationKey(message);
			Assert.state(correlationKey != null,
					"Null correlation not allowed. Maybe the CorrelationStrategy is failing?");
			return correlationKey;
		}

		private Lock obtainGroupLock(Object correlationKey) {
			return AbstractCorrelatingMessageHandler.this.lockRegistry
					.obtain(UUIDConverter.getUUID(correlationKey).toString());
		}

		private ConditionAssessment assessCondition(Message<?> message, MessageGroup group, boolean messagePresent) {
			if (AbstractCorrelatingMessageHandler.this.groupConditionSupplier == null || messagePresent) {
				return new ConditionAssessment(false, group.getCondition());
			}
			return new ConditionAssessment(true,
					AbstractCorrelatingMessageHandler.this.groupConditionSupplier.apply(message, group.getCondition()));
		}

		private MessageGroup createCandidateGroup(MessageGroup group, Message<?> message, boolean messagePresent,
				@Nullable String condition) {

			List<Message<?>> messages = new ArrayList<>(group.getMessages());
			if (!messagePresent) {
				messages.add(message);
			}
			MessageGroup candidate = new SimpleMessageGroup(messages, group.getGroupId(), group.getTimestamp(),
					group.isComplete());
			candidate.setLastModified(group.getLastModified());
			candidate.setLastReleasedMessageSequenceNumber(group.getLastReleasedMessageSequenceNumber());
			candidate.setCondition(condition);
			if (AbstractCorrelatingMessageHandler.this.sequenceAware) {
				MessageGroup sequenceCandidate = new SequenceAwareMessageGroup(candidate);
				sequenceCandidate.setLastReleasedMessageSequenceNumber(candidate.getLastReleasedMessageSequenceNumber());
				sequenceCandidate.setCondition(condition);
				candidate = sequenceCandidate;
			}
			return candidate;
		}

		private void validateMessageDecision(MessageDecision decision, @Nullable MessageAssessment assessment) {
			Assert.state(assessment != null, "No assessment exists for the correlating decision");
			MessageDecision expected;
			if (assessment.getGroupComplete()
					|| (!assessment.getMessagePresent() && !assessment.getCanAdd())) {
				expected = MessageDecision.DISCARD;
			}
			else {
				expected = assessment.getCanReleaseAfterStore()
						? MessageDecision.STORE_AND_RELEASE
						: MessageDecision.STORE_AND_WAIT;
			}
			Assert.state(decision == expected,
					() -> "Correlating agent decision " + decision + " is inconsistent with assessment " + expected);
		}

		private boolean containsMessage(MessageGroup group, Message<?> message) {
			Object messageId = message.getHeaders().get(MessageHeaders.ID);
			return group.getMessages().stream()
					.anyMatch(candidate -> ObjectUtils.nullSafeEquals(messageId,
							candidate.getHeaders().get(MessageHeaders.ID)));
		}

		private long groupVersion(MessageGroup group) {
			if (group.size() == 0 && !group.isComplete() && group.getLastModified() == 0) {
				return 0;
			}
			long version = 31 * group.getTimestamp() + group.getLastModified();
			version = 31 * version + group.size();
			version = 31 * version + (group.isComplete() ? 1 : 0);
			for (Message<?> message : group.getMessages()) {
				version = 31 * version + ObjectUtils.nullSafeHashCode(message.getHeaders().get(MessageHeaders.ID));
			}
			return version;
		}

		private void respond(StreamObserver<ApplyDecisionResponse> responseObserver, DecisionOutcome outcome,
				long currentVersion, String detail) {

			responseObserver.onNext(ApplyDecisionResponse.newBuilder()
					.setOutcome(outcome)
					.setCurrentVersion(currentVersion)
					.setDetail(detail)
					.build());
			responseObserver.onCompleted();
		}

		private StatusRuntimeException toStatusException(RuntimeException exception) {
			String description = exception instanceof MessageDeliveryException
					? "MESSAGE_DELIVERY: " + exception.getMessage()
					: "DEPENDENCY_FAILURE: " + exception.getMessage();
			return Status.INTERNAL.withDescription(description).withCause(exception).asRuntimeException();
		}

	}

	private record ConditionAssessment(boolean evaluated, @Nullable String value) {
	}

	private record OperationContext(Message<?> message, Object correlationKey) {
	}

	protected static class SequenceAwareMessageGroup extends SimpleMessageGroup {

		@Nullable
		private final SimpleMessageGroup sourceGroup;

		public SequenceAwareMessageGroup(MessageGroup messageGroup) {
			/*
			 * Since this group is temporary, and never added to, we simply use the
			 * supplied group's message collection for the lookup rather than creating a
			 * new group.
			 */
			super(messageGroup.getMessages(), null, messageGroup.getGroupId(), messageGroup.getTimestamp(),
					messageGroup.isComplete(), true);
			if (messageGroup instanceof SimpleMessageGroup simpleMessageGroup) {
				this.sourceGroup = simpleMessageGroup;
			}
			else {
				this.sourceGroup = null;
			}
		}

		/**
		 * This method determines whether messages have been added to this group that
		 * supersede the given message based on its sequence id. This can be helpful to
		 * avoid ending up with sequences larger than their required sequence size or
		 * sequences that are missing certain sequence numbers.
		 */
		@Override
		public boolean canAdd(Message<?> message) {
			if (this.size() == 0) {
				return true;
			}
			Integer messageSequenceNumber = message.getHeaders()
					.get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Integer.class);
			if (messageSequenceNumber != null && messageSequenceNumber > 0) {
				Integer messageSequenceSize = message.getHeaders()
						.get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, Integer.class);
				if (messageSequenceSize == null) {
					messageSequenceSize = 0;
				}
				return messageSequenceSize.equals(getSequenceSize())
						&& !(this.sourceGroup != null ? this.sourceGroup.containsSequence(messageSequenceNumber)
						: containsSequenceNumber(this.getMessages(), messageSequenceNumber));
			}
			return true;
		}

		private boolean containsSequenceNumber(Collection<Message<?>> messages, Integer messageSequenceNumber) {
			for (Message<?> member : messages) {
				if (messageSequenceNumber.equals(member.getHeaders().get(
						IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Integer.class))) {
					return true;
				}
			}
			return false;
		}

	}

	private class ForceReleaseMessageGroupProcessor implements MessageGroupProcessor {

		ForceReleaseMessageGroupProcessor() {
		}

		@Override
		@Nullable
		public Object processMessageGroup(MessageGroup group) {
			forceComplete(group);
			return null;
		}

	}

}
