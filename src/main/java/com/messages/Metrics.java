package com.messages;

public class Metrics {

    private int cpu_usage;
    private int memory_usage;
    private int response_time;

    public int getCpu_usage() {
        return cpu_usage;
    }

    public void setCpu_usage(int cpu_usage) {
        this.cpu_usage = cpu_usage;
    }

    public int getMemory_usage() {
        return memory_usage;
    }

    public void setMemory_usage(int memory_usage) {
        this.memory_usage = memory_usage;
    }

    public int getResponse_time() {
        return response_time;
    }

    public void setResponse_time(int response_time) {
        this.response_time = response_time;
    }
}
