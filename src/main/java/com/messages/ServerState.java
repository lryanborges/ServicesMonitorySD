package com.messages;

public class ServerState implements Comparable<ServerState> {

    private String timestamp;
    private String service;
    private String status;
    private String server;
    private Metrics metrics;

    public ServerState() {
        metrics = new Metrics();

        // setar valores iniciais p simulação
        metrics.setCpu_usage(50);
        metrics.setMemory_usage(50);
        metrics.setResponse_time(500);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
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

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public int compareTo(ServerState other) {
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
