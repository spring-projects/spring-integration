package org.springframework.integration.activiti.gateway;

import org.activiti.engine.ProcessEngine;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.integration.MessageChannel;

import org.springframework.transaction.PlatformTransactionManager;


public class ActivityBehaviorMessagingGatewayFactoryBean
        extends AbstractFactoryBean<Object> implements
        BeanFactoryAware, BeanNameAware {
    private volatile boolean updateProcessVariablesFromReplyMessageHeaders = false;
    private volatile boolean forwardProcessVariablesAsMessageHeaders = false;
    private volatile PlatformTransactionManager platformTransactionManager;
    private volatile MessageChannel requestChannel;
    private volatile MessageChannel replyChannel;
    private volatile ProcessEngine processEngine;
    private volatile boolean async;
    private BeanFactory beanFactory;
    private String beanName;

    @SuppressWarnings("unused")
    public void setAsync(boolean async) {
        this.async = async;
    }

    @Override
    protected Object createInstance() throws Exception {

         if (this.async) {
            AsyncActivityBehaviorMessagingGateway asyncActivityBehaviorMessagingGateway = new AsyncActivityBehaviorMessagingGateway();
            asyncActivityBehaviorMessagingGateway.setForwardProcessVariablesAsMessageHeaders(this.forwardProcessVariablesAsMessageHeaders);
            asyncActivityBehaviorMessagingGateway.setUpdateProcessVariablesFromReplyMessageHeaders(this.updateProcessVariablesFromReplyMessageHeaders);
            asyncActivityBehaviorMessagingGateway.setPlatformTransactionManager(this.platformTransactionManager);
            asyncActivityBehaviorMessagingGateway.setReplyChannel(this.replyChannel);
            asyncActivityBehaviorMessagingGateway.setProcessEngine(this.processEngine);
            asyncActivityBehaviorMessagingGateway.setRequestChannel(this.requestChannel);
            asyncActivityBehaviorMessagingGateway.setBeanFactory(this.beanFactory);
            asyncActivityBehaviorMessagingGateway.setBeanName(this.beanName);
            asyncActivityBehaviorMessagingGateway.afterPropertiesSet();

            return asyncActivityBehaviorMessagingGateway;
        } else {
            SyncActivityBehaviorMessagingGateway syncActivityBehaviorMessagingGateway = new SyncActivityBehaviorMessagingGateway();
            syncActivityBehaviorMessagingGateway.setForwardProcessVariablesAsMessageHeaders(this.forwardProcessVariablesAsMessageHeaders);
            syncActivityBehaviorMessagingGateway.setUpdateProcessVariablesFromReplyMessageHeaders(this.updateProcessVariablesFromReplyMessageHeaders);
            syncActivityBehaviorMessagingGateway.setPlatformTransactionManager(this.platformTransactionManager);
            syncActivityBehaviorMessagingGateway.setReplyChannel(this.replyChannel);
            syncActivityBehaviorMessagingGateway.setProcessEngine(this.processEngine);
            syncActivityBehaviorMessagingGateway.setRequestChannel(this.requestChannel);
            syncActivityBehaviorMessagingGateway.setBeanFactory(this.beanFactory);
            syncActivityBehaviorMessagingGateway.setBeanName(this.beanName);
            syncActivityBehaviorMessagingGateway.afterPropertiesSet();

            return syncActivityBehaviorMessagingGateway;
        }
    }

    @SuppressWarnings("unused")
    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @SuppressWarnings("unused")
    public void setUpdateProcessVariablesFromReplyMessageHeaders(boolean updateProcessVariablesFromReplyMessageHeaders) {
        this.updateProcessVariablesFromReplyMessageHeaders = updateProcessVariablesFromReplyMessageHeaders;
    }

    @SuppressWarnings("unused")
    public void setForwardProcessVariablesAsMessageHeaders(boolean forwardProcessVariablesAsMessageHeaders) {
        this.forwardProcessVariablesAsMessageHeaders = forwardProcessVariablesAsMessageHeaders;
    }

    @SuppressWarnings("unused")
    public void setPlatformTransactionManager(PlatformTransactionManager platformTransactionManager) {
        this.platformTransactionManager = platformTransactionManager;
    }

    @SuppressWarnings("unused")
    public void setRequestChannel(MessageChannel requestChannel) {
        this.requestChannel = requestChannel;
    }

    @SuppressWarnings("unused")
    public void setBeanFactory(BeanFactory beanFactory)
        throws BeansException {
        this.beanFactory = beanFactory;
    }

    @SuppressWarnings("unused")
    public void setReplyChannel(MessageChannel replyChannel) {
        this.replyChannel = replyChannel;
    }


    public Class<?> getObjectType() {
        return this.async ? AsyncActivityBehaviorMessagingGateway.class : SyncActivityBehaviorMessagingGateway.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void setBeanName(String name) {
        this.beanName = name;
    }
}
