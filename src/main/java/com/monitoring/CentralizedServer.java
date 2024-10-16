package com.monitoring;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

public class CentralizedServer {

    private static final String LOGSERVER_CONN = "logServerConn";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(LOGSERVER_CONN, BuiltinExchangeType.TOPIC);
        String queueName = channel.queueDeclare().getQueue();

        String routingKey = "#";
        channel.queueBind(queueName, LOGSERVER_CONN, routingKey);

        System.out.println("[*] Aguardando mensagens...");

        DeliverCallback callback = (tagConsumer, entrega) -> {
            String message = new String(entrega.getBody(), "UTF-8");
            System.out.println("Recebida <-- " + entrega.getEnvelope().getRoutingKey() + ": " + message);
        };
        channel.basicConsume(queueName, true, callback, tagConsumer -> {});
    }

}
