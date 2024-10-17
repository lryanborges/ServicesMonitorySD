package com.messages;

public class ServiceOrder implements Comparable<ServiceOrder>{

    private String timestamp;
    private String server;
    private String service;
    private String status;
    private String problem;
    private String action_required;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }

    public String getAction_required() {
        return action_required;
    }

    public void setAction_required(String action_required) {
        this.action_required = action_required;
    }

    @Override
    public int compareTo(ServiceOrder other) {
        if (this.status.equals("vermelho") && !other.status.equals("vermelho")) {
            return -1; // this tem maior prioridade
        } else if (!this.status.equals("vermelho") && other.status.equals("vermelho")) {
            return 1; // other tem maior prioridade
        }

        if (this.status.equals("amarelo") && !other.status.equals("amarelo")) {
            return -1; // amarelo tem prioridade sobre outros
        } else if (!this.status.equals("amarelo") && other.status.equals("amarelo")) {
            return 1; // other tem prioridade
        }

        return 0;
    }
}
