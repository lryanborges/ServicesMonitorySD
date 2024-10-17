package com.monitoring;

import com.cluster.Server;
import com.constants.Colors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.messages.ServerState;
import com.messages.ServiceOrder;
import com.messages.Metrics;
import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public class CentralizedServer {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();

    private static final Map<Instant, ServiceOrder> history[] = new HashMap[6];
    private static final Map<String, ServerState> currentStates = new HashMap<>();
    private static final Set<String> alreadySendOS = new HashSet<>();

    private static final String LOGSERVER_CONN = "logServerConn";
    private static final String MAINTANCE_CONN = "maintenanceConn";
    private static final String[] routingKeys = { "server1/database", "server1/web_server",
                                                "server2/database", "server2/web_server",
                                                "server3/database", "server3/web_server" };
    private static final Scanner scan = new Scanner(System.in);

    public static void main(String[] args) throws Exception {

        // só p inicializar os maps dos históricos
        for (int i = 0; i < history.length; i++) {
            history[i] = new HashMap<>();
        }

        receiveMessage();

        int opc = -1;
        do {
            System.out.println("Servidor de Monitoramento Centralizado");
            System.out.println("---------------------------------------");
            System.out.println("[1] - Verificar status dos servidores");
            System.out.println("[2] - Relatório de incidentes");
            System.out.println("[3] - Serviços com ordens de serviço pendentes");
            System.out.println("[0] - Encerrar o monitoramento");
            System.out.println("---------------------------------------");
            opc = scan.nextInt();

            switch (opc) {
                case 1:

                    if(currentStates.isEmpty()){
                        System.out.println("-----------------------------");
                        System.out.println("Nenhum servidor foi ligado.");
                    }

                    for (String connectionKey : currentStates.keySet()) {
                        System.out.println("-----------------------------");
                        System.out.println("Serviço: " + connectionKey);
                        String status = currentStates.get(connectionKey).getStatus();

                        if(status.equals("azul")) {
                            System.out.println("Status: " + Colors.BLUE + status + Colors.RESET);
                        }
                        if (status.equals("amarelo")) {
                            System.out.println("Status: " + Colors.YELLOW + status + Colors.RESET);
                        }
                        if (status.equals("vermelho")) {
                            System.out.println("Status: " + Colors.RED + status + Colors.RESET);
                        }

                    }

                    System.out.println("-----------------------------");

                    break;
                case 2:

                    System.out.println("---------------------------------------");
                    System.out.println("[1] - Servidor 1 -> Database");
                    System.out.println("[2] - Servidor 1 -> Web Server");
                    System.out.println("[3] - Servidor 2 -> Database");
                    System.out.println("[4] - Servidor 2 -> Web Server");
                    System.out.println("[5] - Servidor 3 -> Database");
                    System.out.println("[6] - Servidor 3 -> Web Server");
                    System.out.println("[7] - Histórico completo");
                    System.out.println("---------------------------------------");
                    opc = scan.nextInt();

                    if(opc >= 1 && opc <= 6) {
                        for (ServiceOrder or : history[opc - 1].values()) {
                            String jsonSO = objectWriter.writeValueAsString(or);
                            System.out.println(jsonSO);
                        }
                    } else if(opc == 7){
                       for (int i = 0; i < history.length; i++) {
                           for (ServiceOrder or : history[i].values()) {
                               String jsonSO = objectWriter.writeValueAsString(or);
                               System.out.println(jsonSO);
                           }
                       }
                    } else {
                        System.out.println("Opção inválida!");
                    }

                    System.out.println("---------------------------------------");

                    break;
                case 3:
                    for (String send : alreadySendOS){
                        System.out.println(send);
                    }
                    break;
                case 0:
                    System.out.println("---------------------------------------");
                    System.out.println("Servidor de Monitoramento Centralizado encerrado.");
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
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(LOGSERVER_CONN, BuiltinExchangeType.TOPIC);
        String queueName = channel.queueDeclare().getQueue();

        // criando a fila de manunteção
        channel.queueDeclare(MAINTANCE_CONN, true, false, false, null);

        for(String routingKey : routingKeys) {
            channel.queueBind(queueName, LOGSERVER_CONN, routingKey);

            DeliverCallback callback = (tagConsumer, entrega) -> {
                //String message = new String(entrega.getBody(), "UTF-8");
                //System.out.println("Recebida <-- " + entrega.getEnvelope().getRoutingKey() + ": " + message);

                String restarted = new String(entrega.getBody(), "UTF-8");

                String topic = entrega.getEnvelope().getRoutingKey();

                // se for sinal de reiniciado, tira a flag e sai da função
                if(restarted.equals("restarted")) {
                    alreadySendOS.remove(topic);
                    return;
                }

                ServerState serverState = objectMapper.readValue(entrega.getBody(), ServerState.class);

                // cada routingkey vai ter seu último estado no map
                currentStates.put(topic, serverState);

                if(serverState.getStatus().equals("amarelo") || serverState.getStatus().equals("vermelho")) {

                    // se já tiver enviado, sai
                    if (alreadySendOS.contains(topic)) {
                        return;
                    }

                    ServiceOrder newServiceOrder = generateServiceOrder(topic, serverState);
                    String jsonServiceOrder = objectWriter.writeValueAsString(newServiceOrder);

                    alreadySendOS.add(topic);

                    channel.basicPublish("", MAINTANCE_CONN,
                            MessageProperties.PERSISTENT_TEXT_PLAIN,
                            jsonServiceOrder.getBytes(StandardCharsets.UTF_8));

                    addToHistory(entrega.getEnvelope().getRoutingKey(), newServiceOrder);

                }

                // oq é pra adicionar no histórico são as ordens de serviço
                //addToHistory(entrega.getEnvelope().getRoutingKey(), serverState);

            };
            channel.basicConsume(queueName, true, callback, tagConsumer -> {});
        }

    }

    private static void addToHistory(String server, ServiceOrder serviceOrder){

        if(server.equals("server1/database")) {
            history[0].put(Instant.parse(serviceOrder.getTimestamp()), serviceOrder);
        } else if(server.equals("server1/web_server")) {
            history[1].put(Instant.parse(serviceOrder.getTimestamp()), serviceOrder);
        } else if(server.equals("server2/database")) {
            history[2].put(Instant.parse(serviceOrder.getTimestamp()), serviceOrder);
        } else if(server.equals("server2/web_server")) {
            history[3].put(Instant.parse(serviceOrder.getTimestamp()), serviceOrder);
        } else if(server.equals("server3/database")) {
            history[4].put(Instant.parse(serviceOrder.getTimestamp()), serviceOrder);
        } else if(server.equals("server3/web_server")) {
            history[5].put(Instant.parse(serviceOrder.getTimestamp()), serviceOrder);
        }

    }

    private static ServiceOrder generateServiceOrder(String topic, ServerState serverState) {

        ServiceOrder serviceOrder = new ServiceOrder();
        Instant timestamp = Instant.now();
        String problem = "";

        serviceOrder.setTimestamp(timestamp.toString());
        serviceOrder.setServer(serverState.getServer());
        serviceOrder.setService(serverState.getService());
        serviceOrder.setStatus(serverState.getStatus());

        Metrics metrics = serverState.getMetrics();
        if(metrics.getCpu_usage() >= 94) { // vermelho
            problem += "Uso de CPU em " + metrics.getCpu_usage() + "%, serviço não responde. ";
        } else if (metrics.getCpu_usage() >= 75){ // amarelo
            problem += "Uso de CPU em " + metrics.getCpu_usage() + "%, serviço requer atenção. ";
        }
        if(metrics.getMemory_usage() >= 90) { // vermelho
            problem += "Uso de Memória em " + metrics.getMemory_usage() + "%, serviço não responde. ";
        } else if(metrics.getMemory_usage() >= 75){ // amarelo
            problem += "Uso de Memória em " + metrics.getMemory_usage() + "%, serviço requer atenção. ";
        }
        if(metrics.getResponse_time() >= 900) { // vermelho
            problem += "Tempo de resposta de " + metrics.getResponse_time() + " segundos, serviço extramente lento. ";
        } else if (metrics.getResponse_time() >= 600) { // amarelo
            problem += "Tempo de resposta de " + metrics.getResponse_time() + " segundos, serviço está lento. ";
        }

        serviceOrder.setProblem(problem);
        serviceOrder.setAction_required("Verificar e reiniciar o serviço.");

        return serviceOrder;
    }

}
