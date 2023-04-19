/*
 * Copyright 2022 Netflix, Inc.
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

// import com.netflix.conductor.core.config.Configuration;

public interface PublisherConfiguration extends ConfigProp {
    String NOTIFICATION_URL_PROPERTY_NAME = "notification.url";
    String NOTIFICATION_URL_DEFAULT_VALUE = "http://bullwinkle.default.svc.cluster.local:7979/v1";
    String NOTIFICATION_ENDPOINT_TASK_PROPERTY_NAME = "notification.endpoint.task";
    String NOTIFICATION_ENDPOINT_TASK_DEFAULT_VALUE = "workflow/TaskNotifications";

    String NOTIFICATION_ENDPOINT_WORKFLOW_PROPERTY_NAME = "notification.endpoint.workflow";
    String NOTIFICATION_ENDPOINT_WORKFLOW_DEFAULT_VALUE = "workflow/WorkflowNotifications";

    String NOTIFICATION_HEADER_DOMAIN_GROUP_PROPERTY_NAME = "notification.header.domain.group";
    String NOTIFICATION_HEADER_DOMAIN_GROUP_DEFAULT_VALUE = "X-Starship-DomainGroup";

    String NOTIFICATION_HEADER_ACCOUNT_COOKIE_PROPERTY_NAME = "notification.header.account.cookie";
    String NOTIFICATION_HEADER_ACCOUNT_COOKIE_DEFAULT_VALUE = "x-barracuda-account";

    String NOTIFICATION_HEADER_PREFER_PROPERTY_NAME = "notification.header.prefer";
    String NOTIFICATION_HEADER_PREFER_DEFAULT_VALUE = "Prefer";

    String NOTIFICATION_HEADER_PREFER_VALUE_PROPERTY_NAME = "notification.header.prefer.value";
    String NOTIFICATION_HEADER_PREFER_VALUE_DEFAULT_VALUE = "respond-async";

    String NOTIFICATION_REQUEST_TIMEOUT_MILLISECOND_CONNECT_PROPERTY_NAME =
            "notification.request.timeout.ms.connect";
    int NOTIFICATION_REQUEST_TIMEOUT_MILLISECOND_CONNECT_DEFAULT_VALUE = 100;

    String NOTIFICATION_REQUEST_TIMEOUT_MILLISECOND_READ_PROPERTY_NAME =
            "notification.request.timeout.ms.read";
    int NOTIFICATION_REQUEST_TIMEOUT_MILLISECOND_READ_DEFAULT_VALUE = 300;

    String NOTIFICATION_REQUEST_TIMEOUT_MILLISECOND_CONNECTION_MANAGER_PROPERTY_NAME =
            "notification.request.timeout.ms.conn.mgr";
    int NOTIFICATION_REQUEST_TIMEOUT_MILLISECOND_CONNECTION_MANAGER_DEFAULT_VALUE = 300;

    String NOTIFICATION_REQUEST_RETRY_COUNT_PROPERTY_NAME = "notification.request.retry.count";
    int NOTIFICATION_REQUEST_RETRY_COUNT_DEFAULT_VALUE = 3;

    String NOTIFICATION_REQUEST_RETRY_INTERVAL_MILLISECOND_PROPERTY_NAME =
            "notification.request.retry.interval.ms";
    int NOTIFICATION_DEFAULT_RETRY_INTERVAL_MILLISECOND_DEFAULT_VALUE = 50;

    String NOTIFICATION_CONNECTION_POOL_MAX_REQUEST_PROPERTY_NAME =
            "notification.connection.pool.max.request";
    int NOTIFICATION_CONNECTION_POOL_MAX_REQUEST_DEFAULT_VALUE = 3;

    String NOTIFICATION_CONNECTION_POOL_MAX_REQUEST_PER_ROUTE_PROPERTY_NAME =
            "notification.connection.pool.max.request.per.route";
    int NOTIFICATION_CONNECTION_POOL_MAX_REQUEST_PER_ROUTE_DEFAULT_VALUE = 3;

    default String getNotificationUrl() {
        return getProperty(NOTIFICATION_URL_PROPERTY_NAME, NOTIFICATION_URL_DEFAULT_VALUE);
    }

    default String getEndPointTask() {
        return getProperty(
                NOTIFICATION_ENDPOINT_TASK_PROPERTY_NAME, NOTIFICATION_ENDPOINT_TASK_DEFAULT_VALUE);
    }

    default String getEndPointWorkflow() {
        return getProperty(
                NOTIFICATION_ENDPOINT_WORKFLOW_PROPERTY_NAME,
                NOTIFICATION_ENDPOINT_WORKFLOW_DEFAULT_VALUE);
    }

    default String getHeaderDomainGroup() {
        return getProperty(
                NOTIFICATION_HEADER_DOMAIN_GROUP_PROPERTY_NAME,
                NOTIFICATION_HEADER_DOMAIN_GROUP_DEFAULT_VALUE);
    }

    default String getHeaderAccountCookie() {
        return getProperty(
                NOTIFICATION_HEADER_ACCOUNT_COOKIE_PROPERTY_NAME,
                NOTIFICATION_HEADER_ACCOUNT_COOKIE_DEFAULT_VALUE);
    }

    default String getHeaderPrefer() {
        return getProperty(
                NOTIFICATION_HEADER_PREFER_PROPERTY_NAME, NOTIFICATION_HEADER_PREFER_DEFAULT_VALUE);
    }

    default String getHeaderPreferValue() {
        return getProperty(
                NOTIFICATION_HEADER_PREFER_VALUE_PROPERTY_NAME,
                NOTIFICATION_HEADER_PREFER_VALUE_DEFAULT_VALUE);
    }

    default int getRequestTimeoutInMillisec() {
        return getIntProperty(
                NOTIFICATION_REQUEST_TIMEOUT_MILLISECOND_CONNECT_PROPERTY_NAME,
                NOTIFICATION_REQUEST_TIMEOUT_MILLISECOND_CONNECT_DEFAULT_VALUE);
    }

    default int getSocketTimeoutInMillisec() {
        return getIntProperty(
                NOTIFICATION_REQUEST_TIMEOUT_MILLISECOND_READ_PROPERTY_NAME,
                NOTIFICATION_REQUEST_TIMEOUT_MILLISECOND_READ_DEFAULT_VALUE);
    }

    default int getConnectionMgrTimeoutInMillisec() {
        return getIntProperty(
                NOTIFICATION_REQUEST_TIMEOUT_MILLISECOND_CONNECTION_MANAGER_PROPERTY_NAME,
                NOTIFICATION_REQUEST_TIMEOUT_MILLISECOND_CONNECTION_MANAGER_DEFAULT_VALUE);
    }

    default int getRequestRetryCount() {
        return getIntProperty(
                NOTIFICATION_REQUEST_RETRY_COUNT_PROPERTY_NAME,
                NOTIFICATION_REQUEST_RETRY_COUNT_DEFAULT_VALUE);
    }

    default int getRequestRetryInterval() {
        return getIntProperty(
                NOTIFICATION_REQUEST_RETRY_INTERVAL_MILLISECOND_PROPERTY_NAME,
                NOTIFICATION_DEFAULT_RETRY_INTERVAL_MILLISECOND_DEFAULT_VALUE);
    }

    default int getConnectionPoolMaxRequest() {
        return getIntProperty(
                NOTIFICATION_CONNECTION_POOL_MAX_REQUEST_PROPERTY_NAME,
                NOTIFICATION_CONNECTION_POOL_MAX_REQUEST_DEFAULT_VALUE);
    }

    default int getConnectionPoolMaxRequestPerRoute() {
        return getIntProperty(
                NOTIFICATION_CONNECTION_POOL_MAX_REQUEST_PER_ROUTE_PROPERTY_NAME,
                NOTIFICATION_CONNECTION_POOL_MAX_REQUEST_PER_ROUTE_DEFAULT_VALUE);
    }
}
