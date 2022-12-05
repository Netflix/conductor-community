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
package io.orkes.conductor.dao.archive;

import java.util.List;

import com.netflix.conductor.common.run.SearchResult;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ScrollableSearchResult<T> extends SearchResult<T> {

    private String queryId;

    public ScrollableSearchResult(List<T> results, String queryId) {
        super(0, results);
        this.queryId = queryId;
    }

    // With ScrollableSearchResult this will always be zero and it's confusing from an API client's
    // perspective.
    // That's why it's ignored.
    @Override
    @JsonIgnore
    public long getTotalHits() {
        return super.getTotalHits();
    }
}
