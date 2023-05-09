/*
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.contribs.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.conductor.core.dal.ExecutionDAOFacade;
import com.netflix.conductor.core.listener.WorkflowStatusListener;

// @ConditionalOnProperty(name = "conductor.webhook.enabled", havingValue = "true")

@Configuration
@EnableConfigurationProperties(ConductorWebhookNotificationProperties.class)
@ConditionalOnProperty(
        name = "conductor.workflow-status-listener.type",
        havingValue = "workflow_publisher")
public class WorkflowStatusPublisherConfiguration {

    private static final Logger log =
            LoggerFactory.getLogger(WorkflowStatusPublisherConfiguration.class);

    @Bean
    public RestClientManager getRestClientManager(ConductorWebhookNotificationProperties config) {
        return new RestClientManager(config);
    }

    @Bean
    public WorkflowStatusListener getWorkflowStatusListener(
            RestClientManager restClientManager, ExecutionDAOFacade executionDAOFacade) {

        return new WorkflowStatusPublisher(restClientManager, executionDAOFacade);
    }
}
