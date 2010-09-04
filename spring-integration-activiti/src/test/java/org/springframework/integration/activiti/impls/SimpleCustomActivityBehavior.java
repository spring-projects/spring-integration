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
package org.springframework.integration.activiti.impls;

import org.activiti.engine.impl.runtime.ExecutionEntity;

import org.activiti.pvm.activity.ActivityBehavior;
import org.activiti.pvm.activity.ActivityExecution;
import org.activiti.pvm.process.PvmTransition;

import org.springframework.beans.factory.InitializingBean;

import java.util.List;


/**
 * A simple component that implements {@link ActivityBehavior}
 *
 * @author Josh Long
 */
public class SimpleCustomActivityBehavior implements ActivityBehavior, InitializingBean {
    public void execute(ActivityExecution dbExecution)
        throws Exception {
        System.out.println("Hello from a custom ActivityBehavior hosted in the Spring context. " + this);


        for (String varName : dbExecution.getVariables().keySet())
            System.out.println(varName + "=" + dbExecution.getVariable(varName));

       
        List<PvmTransition> transitions = dbExecution.getActivity().getOutgoingTransitions();

        dbExecution.take(((null == transitions) || (transitions.size() == 0)) ? null : transitions.get(0));
    }

    public void afterPropertiesSet() throws Exception {
        System.out.println("Starting " + SimpleCustomActivityBehavior.class.getName());
    }
}
