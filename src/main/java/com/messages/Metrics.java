package com.messages;

public class Metrics {

    private int cpu_usage;
    private int memory_usage;
    private int response_time;

    public int getCpu_usage() {
        return cpu_usage;
    }

    public void setCpu_usage(int cpu_usage) {
        if(cpu_usage > 100) {
            this.cpu_usage = 100;
        } else {
            this.cpu_usage = cpu_usage;
        }
    }

    public int getMemory_usage() {
        return memory_usage;
    }

    public void setMemory_usage(int memory_usage) {
        if(memory_usage > 100) {
            this.memory_usage = 100;
        } else {
            this.memory_usage = memory_usage;
        }
    }

    public int getResponse_time() {
        return response_time;
    }

    public void setResponse_time(int response_time) {
        if (response_time > 1000) {
            this.response_time = 1000;
        } else {
            this.response_time = response_time;
        }
    }
}
