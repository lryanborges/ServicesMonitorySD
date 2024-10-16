package com.cluster;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

public class Server {

    private static final String DBSERVER_CONN = "dbServerConn";

    private static final int serverNumber = 3;
    private static final String serverKey = "server" + serverNumber;

    public static void main(String[] args) throws Exception {

        System.out.println("Server started");

        System.out.println("Server " + serverNumber + " iniciado.");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection conn = factory.newConnection();
        final Channel channel = conn.createChannel();

        channel.exchangeDeclare(DBSERVER_CONN, "direct");

        // criar uma fila temporária p vincular à exchange com a chave dos servers
        String temporaryQueue = channel.queueDeclare().getQueue();
        channel.queueBind(temporaryQueue, DBSERVER_CONN, serverKey);

        System.out.println(" [*] Esperando mensagens...");

        DeliverCallback callbackEntrega = (tagConsumidor, environmentRead) -> {
            // essa aq de baixo é p transformar em de json p objeto e ler os valores individualmente mais fácil
            //EnvironmentRead lastRead = objectMapper.readValue(environmentRead.getBody(), EnvironmentRead.class);
            String jsonMessage = new String(environmentRead.getBody(), StandardCharsets.UTF_8);
            System.out.println(" Recebida <-- " + jsonMessage);

            try {
               // fazer algo
            } finally {
                System.out.println("------------------------------------------");
                // faz o reconhecimento das mensagens da fila vinculada
                channel.basicAck(environmentRead.getEnvelope().getDeliveryTag(), false); // tira da fila do RabbitMQ
            }

        };

        // consome as mensagens da fila vinculada
        channel.basicConsume(temporaryQueue, false, callbackEntrega, consumerTag -> { });

    }

}
