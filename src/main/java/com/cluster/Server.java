package com.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.messages.Metrics;
import com.messages.ServerState;
import com.messages.ServiceOrder;
import com.rabbitmq.client.*;

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
    private static final String RESOLVEPROBLEM_CONN = "resolveProblemConn";
    private static Channel channel;
    private static ServerState databaseSS = new ServerState();
    private static ServerState webserverSS = new ServerState();

    private static final int serverNumber = 3;
    private static final String serverKey = "server" + serverNumber;

    public static void main(String[] args) throws Exception {

        System.out.println("Server " + serverNumber + " iniciado.");
        System.out.println("-----------------------");

        sendData("localhost", "database");
        sendData("localhost", "web server");

        receiveResolveMaintance();

    }

    private static void sendData(String ip, String serviceName) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(ip);

        Connection conn = factory.newConnection();
        channel = conn.createChannel();

        channel.exchangeDeclare(LOGSERVER_CONN, BuiltinExchangeType.TOPIC);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                ServerState serverState = getServerState(serverKey, serviceName);

                if (serviceName.equals("database")){
                    databaseSS = serverState;
                }

                if(serviceName.equals("webserver")){
                    webserverSS = serverState;
                }

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
        }, 0, 1, TimeUnit.SECONDS);
    }

    private static String setRoutingKey(String message) {
        if(message.contains("database")) {
            return serverKey + "/database";
        }
        if(message.contains("web server")) {
            return serverKey + "/web_server";
        }

        return null;
    }

    private static ServerState getServerState(String destinyServer, String service) {

        if(databaseSS == null && webserverSS == null) {
            databaseSS = new ServerState();
            webserverSS = new ServerState();
        }

        ServerState state = service.equals("database") ? databaseSS : webserverSS;

        state.setTimestamp(Instant.now().toString());
        state.setService(service);
        state.setServer(destinyServer);

        // definir melhor como pegar essas métricas
        Random rand = new Random();
        Metrics metrics = state.getMetrics();

        int cpuVariation = rand.nextInt(6); // variação entre 0 e 5
        metrics.setCpu_usage(metrics.getCpu_usage() + cpuVariation);

        int memoryVariation = rand.nextInt(6); // variação entre 0 e 5
        metrics.setMemory_usage(metrics.getMemory_usage() + memoryVariation);

        int responseVariation = rand.nextInt(51); // variação entre 0 e 50
        metrics.setResponse_time(metrics.getResponse_time() + responseVariation);

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

    private static void receiveResolveMaintance() throws Exception {

        // Declara a exchange e a fila
        channel.exchangeDeclare(RESOLVEPROBLEM_CONN, BuiltinExchangeType.TOPIC);
        channel.queueDeclare(RESOLVEPROBLEM_CONN, false, false, false, null);

        String queueName = channel.queueDeclare().getQueue();

        String[] serverKeys = { serverKey + "/database", serverKey + "/web_server" };

        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (String sk : serverKeys) {
                    channel.queueBind(queueName, RESOLVEPROBLEM_CONN, sk);

                    DeliverCallback callbackEntrega = (tagConsumidor, entrega) -> {
                        String message = new String(entrega.getBody(), StandardCharsets.UTF_8);

                        System.out.println("msg: " + message);

                        if (message.equals("restarted")) {
                            if (sk.contains("database")) {
                                databaseSS.setMetrics(new Metrics());
                            }

                            if (sk.contains("web_server")) {
                                webserverSS.setMetrics(new Metrics());
                            }

                            System.out.println("RESTARTED");

                            // envia pro servidor central q reiniciou
                            channel.exchangeDeclare(LOGSERVER_CONN, BuiltinExchangeType.TOPIC);
                            channel.basicPublish(
                                    LOGSERVER_CONN,
                                    entrega.getEnvelope().getRoutingKey(),
                                    null,
                                    "restarted".getBytes(StandardCharsets.UTF_8)
                            );

                        }

                        try {
                            // Processar mensagem
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            channel.basicAck(entrega.getEnvelope().getDeliveryTag(), false);
                        }
                    };

                    channel.basicConsume(queueName, false, callbackEntrega, tagConsumidor -> {});
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);

    }

}
