package io.orkes.conductor.dao.archive;

import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.model.WorkflowModel;

public class WorkflowModelSummary extends WorkflowModel {

    public WorkflowModelSummary(String wfId, String parentWfId, String status) {
        super();
        this.setWorkflowId(wfId);
        this.setParentWorkflowId(parentWfId);
        this.setStatus(Status.valueOf(status));
        this.setCreateTime(0L);
        this.setWorkflowDefinition(new WorkflowDef());
    }
}
