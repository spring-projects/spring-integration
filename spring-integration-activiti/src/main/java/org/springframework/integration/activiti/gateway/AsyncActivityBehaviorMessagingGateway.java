/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.activiti.gateway;


import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.bpmn.ReceiveTaskActivity;
import org.activiti.engine.impl.cfg.spring.ProcessEngineFactoryBean;
import org.activiti.pvm.activity.ActivityBehavior;
import org.activiti.pvm.activity.ActivityExecution;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.*;
import org.springframework.integration.activiti.ActivitiConstants;
import org.springframework.integration.activiti.ProcessSupport;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.core.*;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import org.activiti.engine.runtime.Execution ;
import java.util.Map;
import java.util.Set;


/**
 * This class is plugged into an Activiti workflow. <serviceTask /> let's us plugin a custom {@link  ActivityBehavior}.
 * We need to build an {@link  ActivityBehavior} that can send and receive the message,
 * propagating the {@code executionId} and potentially process variables/ header variables.
 * <p/>
 * Simply <code>svn co http://svn.codehaus.org/activiti/activiti/branches/alpha4-spring-integration-adapter</code> that repository and then <code>mvn clean install </code> it.
 * <p/>
 * The class forwards ("pushes") control from a BPM process (in-thread) to a Spring Integration channel where, of course, Spring Integration acts as a client  and
 * can push the execution forward anyway it wants to.
 * <p/>
 * Possible use cases include forwarding the job through an outbound JMS adapter or gateway, forwarding the job through an XMPP adapter, or forwarding the job through
 * to an outbound email adapter.
 * <p/>
 * <p/>
 * The only requirement for the reply is that the {@link org.springframework.integration.Message} arrive on the #replyChannel and that it contain a header of
 * {@link org.springframework.integration.activiti.ActivitiConstants#WELL_KNOWN_EXECUTION_ID_HEADER_KEY} (which the outbound {@link org.springframework.integration.Message} will have)
 * so that the Activiti runtime can signal that execution has completed successfully.
 * <p/>
 * Thanks to Dave Syer and Tom Baeyens for the help brainstorming.
 *
 * @author Josh Long
 * @see ReceiveTaskActivity  the {@link ActivityBehavior} impl that ships w/ Activiti that has the machinery to wake up when signaled
 * @see ProcessEngine the process engine instance is required to be able to use this namespace
 * @see ProcessEngineFactoryBean - use this class to create the aforementioned ProcessEngine instance!
 */
public class AsyncActivityBehaviorMessagingGateway extends ReceiveTaskActivity implements BeanFactoryAware, BeanNameAware, ActivityBehavior, InitializingBean {
	/**
	 * Used to handle sending in a standard way
	 */
	private MessagingTemplate messagingTemplate = new MessagingTemplate();

	/**
	 * This is the channel on which we expect requests - {@link  Execution}s from Activiti - to arrive
	 */
	private volatile MessageChannel requestChannel;

	/**
	 * This is the channel on which we expect to send replies - ie, the result of our work in
	 * Spring Integration - back to Activiti, which should be waiting for the results
	 */
	private volatile MessageChannel replyChannel;

	/**
	 * Injected from Spring or some other mechanism. Recommended approach is through a {@link  ProcessEngineFactoryBean}
	 */
	private volatile ProcessEngine processEngine;

	/**
	 * Should we update the process variables based on the reply {@link org.springframework.integration.Message}'s {@link org.springframework.integration.MessageHeaders}?
	 */
	private volatile boolean updateProcessVariablesFromReplyMessageHeaders = false;

	/**
	 * Should we pass the workflow process variables as message headers when we send a message into the Spring Integration framework?
	 */
	private volatile boolean forwardProcessVariablesAsMessageHeaders = false;

	/**
	 * Forwarded to the {@link org.springframework.integration.core.MessagingTemplate} instance.
	 */
	private volatile PlatformTransactionManager platformTransactionManager;

	/**
	 * A reference to the {@link org.springframework.beans.factory.BeanFactory} that's hosting this component. Spring will inject this reference automatically assuming
	 * this object is hosted in a Spring context.
	 */
	private volatile BeanFactory beanFactory;

	/**
	 * The process engine instance that controls the Activiti PVM. Recommended creation is through {@link ProcessEngineFactoryBean}
	 */
	private RuntimeService processService;

	/**
	 * Provides common logic for things like sifting through inbound message headers and arriving at process variable candidates
	 */
	private ProcessSupport processSupport = new ProcessSupport();
	private String beanName;

	@SuppressWarnings("unused")
	public void setPlatformTransactionManager(PlatformTransactionManager platformTransactionManager) {
		this.platformTransactionManager = platformTransactionManager;
	}

	@SuppressWarnings("unused")
	public void setRequestChannel(MessageChannel requestChannel) {
		this.requestChannel = requestChannel;
	}

	@SuppressWarnings("unused")
	public void setReplyChannel(MessageChannel replyChannel) {
		this.replyChannel = replyChannel;
	}

	@SuppressWarnings("unused")
	public void setProcessEngine(ProcessEngine processEngine) {
		this.processEngine = processEngine;
	}

	@SuppressWarnings("unused")
	public void setForwardProcessVariablesAsMessageHeaders(boolean forwardProcessVariablesAsMessageHeaders) {
		this.forwardProcessVariablesAsMessageHeaders = forwardProcessVariablesAsMessageHeaders;
	}

	@SuppressWarnings("unused")
	public void setUpdateProcessVariablesFromReplyMessageHeaders(boolean updateProcessVariablesFromReplyMessageHeaders) {
		this.updateProcessVariablesFromReplyMessageHeaders = updateProcessVariablesFromReplyMessageHeaders;
	}

	public void setBeanFactory(BeanFactory beanFactory)
			throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void signal(ActivityExecution execution, String signalName, Object data) throws Exception {

		if (data instanceof MessageHeaders) {
			MessageHeaders messageHeaders = (MessageHeaders) data;

			if (this.updateProcessVariablesFromReplyMessageHeaders) {
				Map<String, Object> vars = execution .getVariables();
				Set<String> existingVars = vars.keySet();

				Map<String, Object> procVars = this.processSupport.processVariablesFromMessageHeaders(existingVars, messageHeaders);

				for (String key : procVars.keySet())
					execution.setVariable(key, procVars.get(key));
			}
		}

		super.signal(execution, signalName, data);
	}

	/**
	 * This is the main interface method from {@link ActivityBehavior}. It will be called when the BPMN process executes the node referencing this logic.
	 *
	 * @param execution the {@link  ActivityExecution} as given to use by the engine
	 * @throws Exception
	 */
	public void execute(ActivityExecution execution) throws Exception {
		String executionId = execution.getId();

		MessageBuilder<?> messageBuilder = MessageBuilder.withPayload(execution).setHeader(ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY, executionId).setCorrelationId(executionId);

		if (this.forwardProcessVariablesAsMessageHeaders) {
			Map<String, Object> variables =  execution.getVariables();

			if ((variables != null) && (variables.size() > 0)) {
				messageBuilder = messageBuilder.copyHeadersIfAbsent(variables);
			}
		}

		Message<?> msg = messageBuilder.setReplyChannel(replyChannel).build();

		this.messagingTemplate.send(this.requestChannel, msg);
	}

	/**
	 * Verify the presence of references to a request and reply {@link  org.springframework.integration.MessageChannel},
	 * the {@link  ProcessEngine}, and setup the {@link org.springframework.integration.core.MessageHandler} that handles the replies
	 *
	 * @throws Exception
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.state(this.replyChannel != null, "'replyChannel' can't be null!");
		Assert.state(this.requestChannel != null, "'requestChannel' can't be null!");
		Assert.state(this.processEngine != null, "'processEngine' can't be null!");

		processService = this.processEngine.getRuntimeService();

		if (this.platformTransactionManager != null) {
			this.messagingTemplate.setTransactionManager(this.platformTransactionManager);
		}

		MessageHandler handler = new ReplyMessageHandler();

		PollerMetadata pollerMetadata = new PollerMetadata();
		pollerMetadata.setReceiveTimeout(-1);
		pollerMetadata.setTransactionManager(this.platformTransactionManager);
		pollerMetadata.setTrigger(new PeriodicTrigger(10));

		ConsumerEndpointFactoryBean consumerEndpointFactoryBean = new ConsumerEndpointFactoryBean();
		consumerEndpointFactoryBean.setAutoStartup(false);

		if (this.replyChannel instanceof PollableChannel)
			consumerEndpointFactoryBean.setPollerMetadata(pollerMetadata);

		consumerEndpointFactoryBean.setBeanFactory(this.beanFactory);
		consumerEndpointFactoryBean.setHandler(handler);
		consumerEndpointFactoryBean.setInputChannel(this.replyChannel);
		consumerEndpointFactoryBean.setBeanName(this.beanName);

		AbstractEndpoint correlator = consumerEndpointFactoryBean.getObject();

		if (correlator != null) {
			correlator.start();
		}
	}

	public void setBeanName(String s) {
		this.beanName = s;
	}

	/**
	 * This class listens for results on the reply channel and causes the flow of execution to proceed inside the business process
	 */
	class ReplyMessageHandler implements MessageHandler {
		public void handleMessage(Message<?> message) throws   MessageHandlingException, MessageDeliveryException {
			try {
				MessageHeaders messageHeaders = message.getHeaders();
				String executionId = (String) message.getHeaders().get(ActivitiConstants.WELL_KNOWN_EXECUTION_ID_HEADER_KEY);
				Execution execution = processService.findExecutionById(executionId);
				processEngine.getRuntimeService().signal(execution.getId(), AsyncActivityBehaviorMessagingGateway.class.getName() , messageHeaders);
			} catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		}
	}
}
