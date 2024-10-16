package com.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.messages.Metrics;
import com.messages.ServerState;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.Random;
import java.time.Instant;

public class Server {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final String LOGSERVER_CONN = "logServerConn";

    private static final int serverNumber = 1;
    private static final String serverKey = "server" + serverNumber;

    public static void main(String[] args) throws Exception {

        System.out.println("Server started");

        System.out.println("Server " + serverNumber + " iniciado.");

        receiveData("localhost", "database");
        receiveData("localhost", "web server");

    }

    private static void receiveData(String ip, String serviceName) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(ip);

        Connection conn = factory.newConnection();
        final Channel channel = conn.createChannel();

        channel.exchangeDeclare(LOGSERVER_CONN, BuiltinExchangeType.TOPIC);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                ServerState serverState = getServerState(serverKey, serviceName);
                String jsonMessage = objectWriter.writeValueAsString(serverState);
                String routingKey = setRoutingKey(jsonMessage);

                channel.basicPublish(
                    LOGSERVER_CONN,
                    routingKey,
                    null,
                    jsonMessage.getBytes(StandardCharsets.UTF_8)
                );
                System.out.println("Enviando --> " + jsonMessage);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private static String setRoutingKey(String message) {
        if(message.contains("server1") && message.contains("database")) {
            return "server1/database";
        }
        else if(message.contains("server2") && message.contains("database")) {
            return "server2/database";
        }
        else if(message.contains("server3") && message.contains("database")) {
            return "server3/database";
        }
        else if(message.contains("server1") && message.contains("web server")) {
            return "server1/web_server";
        }
        else if(message.contains("server2") && message.contains("web server")) {
            return "server2/web_server";
        }
        else if(message.contains("server3") && message.contains("web server")) {
            return "server3/web_server";
        }

        return null;
    }

    private static ServerState getServerState(String destinyServer, String service) {

        ServerState state = new ServerState();

        state.setTimestamp(Instant.now().toString());
        state.setService(service);
        state.setServer(destinyServer);

        // definir melhor como pegar essas métricas
        Random rand = new Random();
        Metrics metrics = new Metrics();
        metrics.setCpu_usage(rand.nextInt(100));
        metrics.setMemory_usage(rand.nextInt(100));
        metrics.setResponse_time(rand.nextInt(1000));

        state.setMetrics(metrics);

        if(metrics.getCpu_usage() >= 94 ||
        metrics.getMemory_usage() >= 90 ||
        metrics.getResponse_time() >= 900) {
            state.setStatus("vermelho");
        }
        else if((metrics.getCpu_usage() >= 75 && metrics.getCpu_usage() < 94) ||
        (metrics.getMemory_usage() >= 75 && metrics.getMemory_usage() < 90) ||
        metrics.getResponse_time() >= 600 && metrics.getResponse_time() < 900) {
            state.setStatus("amarelo");
        }
        else {
            state.setStatus("azul");
        }

        return state;
    }

}
