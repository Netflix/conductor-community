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

import java.util.*;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.en.EnglishAnalyzer;

import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndexValueExtractor {

    private static final String splitWords = "\"|,|;|\\s|,";
    private static final String replaceWords = "\"|,|;|\\s|,|:";
    private static final Collection stopWords = EnglishAnalyzer.getDefaultStopSet();
    private static final String ROOT_WF = "root_wf";
    
    public static Collection<String> getIndexWords(
            WorkflowModel workflow, int maxWords, int maxWordLength) {

        try {

            List<String> words = getIndexWords(workflow);
            return words.stream()
                    .flatMap(value -> Arrays.asList(value.split(splitWords)).stream())
                    .filter(word -> word.length() < maxWordLength)
                    .filter(word -> word.length() > 2)
                    .filter(word -> !word.trim().isBlank())
                    .map(word -> word.toLowerCase().trim().replaceAll(replaceWords + "+$", ""))
                    .filter(word -> !stopWords.contains(word))
                    .limit(maxWords)
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            log.warn("Error serializing input/output map to text: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private static List<String> getIndexWords(WorkflowModel workflow) {

        List<String> words = new ArrayList<>();
        append(words, workflow.getCorrelationId());
        append(words, workflow.getInput());
        append(words, workflow.getOutput());
        append(words, workflow.getReasonForIncompletion());
        append(words, workflow.getVariables());

        if (workflow.getParentWorkflowId() == null) {
            append(words, ROOT_WF);
        }

        for (TaskModel task : workflow.getTasks()) {
            append(words, task.getOutputData());
        }
        return words;
    }

    private static void append(List<String> words, Object value) {
        if (value instanceof String || value instanceof Number) {
            if (value != null) words.add(value.toString());
        } else if (value instanceof List) {
            List values = (List) value;
            for (Object valueObj : values) {
                if (valueObj != null) words.add(valueObj.toString());
            }
        } else if (value instanceof Map) {
            append(words, (Map) value);
        }
    }

    private static void append(List<String> words, Map<String, Object> map) {

        map.values()
                .forEach(
                        value -> {
                            if (value instanceof String || value instanceof Number) {
                                if (value != null) words.add(value.toString());
                            } else if (value instanceof Map) {
                                append(words, (Map) value);
                            } else if (value instanceof List) {
                                List values = (List) value;
                                for (Object valueObj : values) {
                                    if (valueObj != null) words.add(valueObj.toString());
                                }
                            }
                        });
    }
}
