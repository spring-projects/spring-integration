package org.springframework.integration.activiti.signup;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;


@Component
public class SignupClient {
    private TaskService taskService;
    private RuntimeService runtimeService;
    private RepositoryService repositoryService;

    @Resource
    private ProcessEngine processEngine;

    @PostConstruct
    public void start() throws Exception {
        this.taskService = this.processEngine.getTaskService();
        this.repositoryService = this.processEngine.getRepositoryService();
        this.runtimeService = this.processEngine.getRuntimeService();

        repositoryService.createDeployment().addClasspathResource("processes/signup.bpmn20.xml").deploy();
    }

    private List<Task> tasksForUser(String user, String name)
        throws Exception {
        return taskService.createTaskQuery().name(name).assignee(user).list();
    }

    private boolean handleWorkForUser(String user, String taskName)
        throws Exception {

	    boolean workToBeDone = false;
        for (Task t : tasksForUser(user, taskName)) {
	        workToBeDone = true ;
            System.out.println("starting " + t.getId() + " : " + t.getName());
            taskService.complete(t.getId());
            System.out.println("completed " + t.getId() + " : " + t.getName());
        }
	    return workToBeDone ;
    }

    public void startSignupProcess(long customerId) throws Exception {
        Map<String, Object> parms = new HashMap<String, Object>();
        parms.put("customerId", customerId);

        runtimeService.startProcessInstanceByKey("signup", parms);
    }

    public static void main(String[] args) throws Throwable {
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("/org/springframework/integration/activiti/SignupTest-context.xml");
        classPathXmlApplicationContext.start();

        SignupClient signupClient = classPathXmlApplicationContext.getBean(SignupClient.class);
        signupClient.startSignupProcess(System.currentTimeMillis());
        signupClient.handleWorkForUser("customer", "sign-up");

	    while(signupClient.handleWorkForUser( "customer", "fix-errors"))
		    ;

	    signupClient.handleWorkForUser("customer", "confirm-email");
	    
	    


    }
}
