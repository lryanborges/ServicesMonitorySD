package com.maintance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.messages.ServiceOrder;
import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServiceMaintance {

    private static final Technical[] technicals = {
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

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final Queue<Technical> technicalQueue = new LinkedList<>();
    private static final PriorityQueue<ServiceOrder> maintanceQueue = new PriorityQueue<>();
    private static final List<String> currentServiceOrders = new ArrayList<>();

    private static final String MONITORINGSERVER_CONN = "maintenanceConn";
    private static final Scanner scan  = new Scanner(System.in);

    public static void main(String[] args) throws Exception {

        for(Technical tec : technicals) {
            if(tec.isAvailable())
                technicalQueue.add(tec);
        }

        setTechnicalMaintance();

        receiveMessage();

        int opc = -1;

        do {

            System.out.println("\tManutenção de Serviços");
            System.out.println("---------------------------------------");
            System.out.println("[1] - Verificar ordens de serviço em aberto");
            System.out.println("[2] - Verificar técnicos disponíveis");
            System.out.println("[3] - Verificar manutenções em execução");
            System.out.println("[0] - Encerrar o sistema de manutenção");
            System.out.println("---------------------------------------");
            opc = scan.nextInt();

            switch (opc) {
                case 1:

                    for(ServiceOrder so : maintanceQueue) {
                        String jsonSO = objectWriter.writeValueAsString(so);
                        System.out.println(jsonSO);
                    }

                    break;
                case 2:

                    for(Technical tec : technicalQueue) {
                        System.out.println(" - " + tec.getName());
                    }

                    break;
                case 3:

                    for(String currentSO : currentServiceOrders) {
                        System.out.println(" - " + currentSO);
                    }

                    break;
                case 0:
                    System.out.println("---------------------------------------");
                    System.out.println("Sistema de manutenção encerrado.");
                    System.out.println("---------------------------------------");

                    System.exit(0);
                    break;
                default:
                    System.out.println("Opção inválida!");
            }

        } while(opc != 0);

    }

    private static void receiveMessage() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection conn = factory.newConnection();

        final Channel channel = conn.createChannel();

        channel.queueDeclare(MONITORINGSERVER_CONN, true, false, false, null);

        channel.basicQos(1);

        DeliverCallback callbackEntrega = (tagConsumidor, environmentRead) -> {

            // recebe a mensagem pelo corpo
            String jsonMessage = new String(environmentRead.getBody(), StandardCharsets.UTF_8);
            //System.out.println("Recebida <-- " + jsonMessage);

            ServiceOrder newServiceOrder = objectMapper.readValue(environmentRead.getBody(), ServiceOrder.class);
            maintanceQueue.add(newServiceOrder);

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

    private static void setTechnicalMaintance() {

        // pra toda hora ficar tentando enviar técnico caso precise
        scheduler.scheduleAtFixedRate(() -> {

            if(maintanceQueue.isEmpty() || technicalQueue.isEmpty()) {
                return;
            }

            Technical tec = technicalQueue.poll();
            ServiceOrder so = maintanceQueue.poll();

            currentServiceOrders.add("Técnico " + tec.getName() + " está realizando manutenção no " + so.getService() + " do " + so.getServer());
            //System.out.println("Técnico " + tec.getName() + " está indo realizar manutenção no " + so.getService() + " do " + so.getServer());

            // dps de um tempo ele termina a os e volta pra fila
            scheduler.schedule(() -> {
                technicalQueue.add(tec);

                currentServiceOrders.removeIf(order -> order.contains(tec.getName()));
            }, 10, TimeUnit.SECONDS);

        }, 0, 1, TimeUnit.SECONDS);
    }

}
