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

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;

public class TestScheduler {

    public static void main(String[] args) throws InterruptedException {
        RestTemplate rt =
                new RestTemplateBuilder()
                        .setReadTimeout(Duration.ofMillis(7000))
                        .setConnectTimeout(Duration.ofMillis(7000))
                        .build();

        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName("first_flow_all_simple");
        DecimalFormat df = new DecimalFormat("000");
        ExecutorService es = Executors.newFixedThreadPool(100);
        int start = 1;
        int maxSet = 150;
        String host = "https://perf6.conductorworkflow.net/";
        CountDownLatch latch = new CountDownLatch(maxSet);
        HttpHeaders headers = new HttpHeaders();
        headers.put(
                "X-Authorization",
                Collections.singletonList(
                        "eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIiwiaXNzIjoiaHR0cHM6Ly9vcmtlcy10ZXN0LnVzLmF1dGgwLmNvbS8ifQ..Hhotod4LBfYqEZBc.AFl2Xa_AYk4NaWJzakRjuMx_wpmj-RSLaf7RzzFZxdo35jW3fXzA7RQ8pOevieLWZKgMRYPc6FWdyoFbIZXFUYZcQPUk83UzsigXQSB4g1PUUslmxv6betHQtBNt0PYaNLzwI5PF7QkzNWEdeIPa2b-IsTXniagk_fFeTRJmMdPmfqDDqGmU6kd1v3M12JOqVp5hNXGJirErz-9tB8uOySPXFVbiTCbz8mk_JA-B4LTUWzPzdyE6J7QqSHmsqjQZfkPNpCTYnEF958xLn1x3vZ2K9d84YYSYQTPU_ce3lZJeI3RbfoOp2fQL6KWzPIcPujRh.NdgKCYUDzhIZzfHinmMQdg"));
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (int i = 0; i < 60; i++) {
            int finalI = i;
            es.submit(
                    () -> {
                        try {
                            for (int j = start; j <= maxSet; j++) {
                                Map<String, Object> sp = new HashMap<>();
                                sp.put(
                                        "name",
                                        String.format(
                                                "test_second_%s_%s",
                                                df.format(j), df.format(finalI)));
                                System.out.println(
                                        String.format(
                                                "test_second_%s_%s",
                                                df.format(j), df.format(finalI)));

                                // rt.exchange("https://perf6.conductorworkflow.net/api/scheduler/schedules/" + sp.get("name"), HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

                                sp.put("cronExpression", String.format("%s * * ? * *", finalI));
                                sp.put("startWorkflowRequest", request);
                                HttpEntity<Map<String, Object>> entity =
                                        new HttpEntity<>(sp, headers);

                                ResponseEntity<Void> voidResponseEntity =
                                        rt.exchange(
                                                host + "api/scheduler/schedules",
                                                HttpMethod.POST,
                                                entity,
                                                Void.class);
                                System.out.println(voidResponseEntity.getStatusCode());
                            }
                        } catch (RestClientException e) {
                            e.printStackTrace();
                        } finally {
                            latch.countDown();
                            System.out.println("latch: " + latch.getCount());
                        }
                    });
        }
        latch.await();
        System.out.println("Latch: " + latch.getCount());
        System.out.println("Done");
    }

    private static void createWorkflow(RestTemplate rt, HttpHeaders headers) {
        Map<String, Object> sp = new HashMap<>();
        sp.put("name", "first_flow");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(sp, headers);
        ResponseEntity<String> exchange =
                rt.exchange(
                        "https://perf5.orkesconductor.com/api/workflow",
                        HttpMethod.DELETE,
                        entity,
                        String.class);
        System.out.println(exchange.getStatusCode());
    }
}
