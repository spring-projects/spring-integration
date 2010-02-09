/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.jdbc;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcOperations;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;

/**
 * A polling channel adapter that creates messages from the payload returned by executing a select query
 * Optionally an update can be executed after the select in order to update processed rows
 *
 * @author Jonas Partner
 */
public class JdbcPollingChannelAdapter implements MessageSource<Object>, InitializingBean {

    private final SimpleJdbcOperations jdbcOperations;

    private final String selectQuery;

    private volatile RowMapper<?> rowMapper;

    private volatile String updateQuery;

    private volatile SqlParameterSource sqlQueryParameterSource;

    private volatile TransactionDefinition transactionDefinition;

    private volatile TransactionTemplate transactionTemplate;

    private volatile PlatformTransactionManager platformTransactionManager;

    public JdbcPollingChannelAdapter(DataSource dataSource, String selectQuery) {
        this.jdbcOperations = new SimpleJdbcTemplate(dataSource);
        this.selectQuery = selectQuery;
    }

    public JdbcPollingChannelAdapter(SimpleJdbcOperations jdbcOperations, String selectQuery) {
        this.jdbcOperations = jdbcOperations;
        this.selectQuery = selectQuery;
    }

    public void setTransactionDefinition(TransactionDefinition transactionDefinition) {
        this.transactionDefinition = transactionDefinition;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.platformTransactionManager = platformTransactionManager;
    }

    public void setRowMapper(RowMapper<?> rowMapper) {
        this.rowMapper = rowMapper;
    }

    public void setUpdateQuery(String updateQuery) {
        this.updateQuery = updateQuery;
    }


    public void afterPropertiesSet() throws Exception {
        if (this.transactionDefinition == null) {
            this.transactionDefinition = new DefaultTransactionDefinition();
        }

        if (this.platformTransactionManager != null) {
            this.transactionTemplate = new TransactionTemplate(this.platformTransactionManager, this.transactionDefinition);
        }
    }

    public Message<Object> receive() {
        Object payload = null;
        if (this.transactionTemplate != null){
            payload = this.transactionTemplate.execute(new TransactionCallback<Object>(){
                public Object doInTransaction(TransactionStatus status) {
                    return pollAndUpdate();
                }
            });
        } else {
            payload = pollAndUpdate();
        }
        return MessageBuilder.withPayload(payload).build();
    }

    protected Object pollAndUpdate(){
        List payload;
           if (this.rowMapper != null) {
                payload = pollWithRowMapper();
           } else {
               payload = this.jdbcOperations.queryForList(this.selectQuery, this.sqlQueryParameterSource);
           }

        return payload;

    }

    protected List pollWithRowMapper(){
        List payload = null;
        if(this.sqlQueryParameterSource != null){
            payload = this.jdbcOperations.query(this.selectQuery, this.rowMapper, this.sqlQueryParameterSource);
        } else {
            payload = this.jdbcOperations.query(this.selectQuery, this.rowMapper);
        }
        return payload;
    }

    protected Object pollForListOfMap(){
        List payload = null;
            if(this.sqlQueryParameterSource != null){
                payload = this.jdbcOperations.queryForList(this.selectQuery,  this.sqlQueryParameterSource);
            } else {
                payload = this.jdbcOperations.queryForList(this.selectQuery);
            }
            return payload;
    }


}
