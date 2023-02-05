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
package com.netflix.conductor.oracle.dao;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.netflix.conductor.common.config.TestObjectMapperConfiguration;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.dao.ExecutionDAO;
import com.netflix.conductor.dao.ExecutionDAOTest;
import com.netflix.conductor.model.WorkflowModel;
import com.netflix.conductor.oracle.config.OracleTestConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ContextConfiguration(
        classes = {
            TestObjectMapperConfiguration.class,
            OracleTestConfiguration.class,
            FlywayAutoConfiguration.class
        })
@RunWith(SpringRunner.class)
@SpringBootTest
public class OracleExecutionDAOTest extends ExecutionDAOTest {

    @Autowired private OracleExecutionDAO executionDAO;

    @Autowired Flyway flyway;

    // clean the database between tests.
    @Before
    public void before() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    public void testPendingByCorrelationId() {

        WorkflowDef def = new WorkflowDef();
        def.setName("pending_count_correlation_jtest");

        WorkflowModel workflow = createTestWorkflow();
        workflow.setWorkflowDefinition(def);

        generateWorkflows(workflow, 10);

        List<WorkflowModel> bycorrelationId =
                getExecutionDAO()
                        .getWorkflowsByCorrelationId(
                                "pending_count_correlation_jtest", "corr001", true);
        assertNotNull(bycorrelationId);
        assertEquals(10, bycorrelationId.size());
    }

    @Override
    public ExecutionDAO getExecutionDAO() {
        return executionDAO;
    }
}
