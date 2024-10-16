package com.services;

import com.cluster.Server;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.messages.Metrics;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import com.messages.ServerState;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Database {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static String status = "azul";
    private static final String SERVERS_CONN = "dbServerConn";

    public static void main(String[] args) throws Exception {

        System.out.println("Database started");
        System.out.println("-----------------");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection conn = factory.newConnection();
        final Channel channel = conn.createChannel();

        channel.exchangeDeclare(SERVERS_CONN, "direct");

        String[] serverKeys = {"server1", "server2", "server3"};

        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (String serverKey : serverKeys) {
                    // publica a mensagem com uma chave de roteamento específica
                    ServerState serverState = generateServerState(serverKey);
                    String jsonMessage = objectWriter.writeValueAsString(serverState);

                    channel.basicPublish(SERVERS_CONN, serverKey, null, jsonMessage.getBytes("UTF-8"));
                    System.out.println(" --> Encaminhado ao servidor com chave: '" + serverKey + "'");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);

    }

    private static ServerState generateServerState(String destinyServer) {

        ServerState state = new ServerState();

        state.setTimestamp(Instant.now().toString());
        state.setService("database");
        state.setStatus(status);
        state.setServer(destinyServer);

        // definir melhor como pegar essas métricas
        Metrics metrics = new Metrics();
        metrics.setCpu_usage(85);
        metrics.setMemory_usage(70);
        metrics.setResponse_time(300);

        state.setMetrics(metrics);

        return state;
    }

}
