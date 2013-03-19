/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.springframework.integration.kafka.support;

import kafka.consumer.ConsumerConfig;
import org.springframework.beans.factory.FactoryBean;

import java.util.Properties;

/**
 * @author Soby Chacko
 */
public class ConsumerConfigFactoryBean implements FactoryBean<ConsumerConfig> {

    private final ConsumerMetadata consumerMetadata;
    private final ZookeeperConnect zookeeperConnect;

    public ConsumerConfigFactoryBean(final ConsumerMetadata consumerMetadata,
                                     final ZookeeperConnect zookeeperConnect){
        this.consumerMetadata = consumerMetadata;
        this.zookeeperConnect = zookeeperConnect;
    }

    @Override
    public ConsumerConfig getObject() throws Exception {
        final Properties properties = new Properties();
        properties.put("zookeeper.connect", zookeeperConnect.getZkConnect());
        properties.put("zookeeper.session.timeout.ms", zookeeperConnect.getZkSessionTimeout());
        properties.put("zookeeper.sync.time.ms", zookeeperConnect.getZkSyncTime());
        properties.put("auto.commit.interval.ms", consumerMetadata.getAutoCommitInterval());
        properties.put("consumer.timeout.ms", consumerMetadata.getConsumerTimeout());
        properties.put("group.id", consumerMetadata.getGroupId());

        return new ConsumerConfig(properties);
    }

    @Override
    public Class<?> getObjectType() {
        return ConsumerConfig.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
