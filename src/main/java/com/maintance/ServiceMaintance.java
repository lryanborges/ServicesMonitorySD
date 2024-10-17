package com.maintance;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public class ServiceMaintance {

    private final Technical[] technicals = {
            new Technical("Ryan"),
            new Technical("Vinicius"),
            new Technical("Paulo Henrique"),
            new Technical("Valentina"),
            new Technical("Davi"),
            new Technical("Galdino"),
            new Technical("Kevyn"),
            new Technical("Whesley"),
            new Technical("Arthur"),
    };

    private static final String MONITORINGSERVER_CONN = "maintenanceConn";

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection conn = factory.newConnection();

        final Channel channel = conn.createChannel();

        channel.queueDeclare(MONITORINGSERVER_CONN, true, false, false, null);

        channel.basicQos(1);

        DeliverCallback callbackEntrega = (tagConsumidor, environmentRead) -> {

            // recebe a mensagem pelo corpo
            String jsonMessage = new String(environmentRead.getBody(), StandardCharsets.UTF_8);
            System.out.println("Recebida <-- " + jsonMessage);

            try {
                // fazer algo
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                channel.basicAck(environmentRead.getEnvelope().getDeliveryTag(), false); // tira da fila do RabbitMQ
            }
        };

        channel.basicConsume(MONITORINGSERVER_CONN, false, callbackEntrega, tagConsumidor -> { });

    }

}
