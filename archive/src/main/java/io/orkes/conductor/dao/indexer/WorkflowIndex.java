/*
 * Copyright 2022 Orkes, Inc.
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
package io.orkes.conductor.dao.indexer;

import java.util.Collection;

import com.netflix.conductor.model.WorkflowModel;

public class WorkflowIndex {

    private WorkflowModel workflow;
    private int maxWords;
    private int maxWordLength;

    public WorkflowIndex(WorkflowModel workflow, int maxWords, int maxWordLength) {
        this.workflow = workflow;
        this.maxWords = maxWords;
        this.maxWordLength = maxWordLength;
    }

    public Collection<String> toIndexWords() {
        return IndexValueExtractor.getIndexWords(workflow, maxWords, maxWordLength);
    }

    @Override
    public String toString() {
        return toIndexWords().toString();
    }
}
