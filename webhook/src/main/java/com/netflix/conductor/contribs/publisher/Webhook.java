package com.netflix.conductor.contribs.publisher;

public class Webhook {

    private String url;

    private String endpointTask;

    private String endpointWorkflow;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEndpointTask() {
        return endpointTask;
    }

    public void setEndpointTask(String endpointTask) {
        this.endpointTask = endpointTask;
    }

    public String getEndpointWorkflow() {
        return endpointWorkflow;
    }

    public void setEndpointWorkflow(String endpointWorkflow) {
        this.endpointWorkflow = endpointWorkflow;
    }

}