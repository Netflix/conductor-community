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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.config.ObjectMapperProvider;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import static io.orkes.conductor.dao.indexer.IndexValueExtractor.getIndexWords;
import static org.junit.jupiter.api.Assertions.*;

public class TestIndexValueExtractor {

    @Test
    public void testIndexer() {
        WorkflowModel model = new WorkflowModel();
        model.getTasks().add(new TaskModel());

        String uuid = UUID.randomUUID().toString();
        model.getInput().put("correlation_id", uuid);
        System.out.println("correlationId: " + uuid);
        model.getInput().put("keep", "abcd");

        String json =
                "{\n"
                        + "  \"response\": \"java.lang.Exception: I/O error on GET request for \\\"https://orkes-services.web.app2/data.json\\\": orkes-services.web.app2: nodename nor servname provided, or not known; nested exception is java.net.UnknownHostException: orkes-services.web.app2: nodename nor servname provided, or not known\"\n"
                        + "}";
        try {
            Map output = new ObjectMapperProvider().getObjectMapper().readValue(json, Map.class);
            model.getOutput().putAll(output);
        } catch (Exception e) {

        }

        for (int i = 0; i < 100; i++) {
            model.getTasks().get(0).getOutputData().put("id" + i, UUID.randomUUID().toString());
        }
        Collection<String> words = getIndexWords(model, 3, 50);

        // Sine all the UUIDs are longer than max word length, they should all get filtered out
        assertNotNull(words);
        assertEquals(3, words.size());
        assertTrue(words.contains("abcd"));
        assertTrue(words.contains(uuid), uuid + " not in the list of words : " + words);

        words = getIndexWords(model, 200, 50);
        System.out.println(words);
        System.out.println(words.size());
        words.stream().forEach(System.out::println);

        // All UUIDs shouldbe present
        assertNotNull(words);
        assertTrue(words.contains("https://orkes-services.web.app2/data.json"));
        assertTrue(words.contains(uuid));
    }

    @Test
    void testWithinIndexLimit() {
        final WorkflowModel workflow = new WorkflowModel();
        workflow.setInput(generateInput(2));
        workflow.setOutput(generateInput(2));
        final Collection<String> indexWords = getIndexWords(workflow, 10, 10);
        final LinkedHashSet<String> expected = new LinkedHashSet(List.of("root_wf", "value0", "value1"));
        assertEquals(expected, indexWords);
    }

    @Test
    void tesOverIndexLimit() {
        final WorkflowModel workflow = new WorkflowModel();
        workflow.setInput(generateInput(10));
        workflow.setOutput(generateInput(10));
        final Collection<String> indexWords = getIndexWords(workflow, 2, 10);
        final LinkedHashSet<String> expected = new LinkedHashSet(List.of("root_wf", "value0"));
        assertEquals(expected, indexWords);
    }

    private static Map<String, Object> generateInput(int length) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < length; i++) {
            map.put(Integer.toString(i), "value" + i);
        }
        return map;
    }
}
