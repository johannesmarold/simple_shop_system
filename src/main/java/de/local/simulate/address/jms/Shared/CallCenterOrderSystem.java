package de.local.simulate.address.jms.Shared;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

// PATTERN DESCRIPTION
// CallCenterOrderSystem generates orders and writes them to a text file.
// A File Component in Camel acts as a Channel Adapter, reading from this file and sending the orders
// to the same incomingOrders queue as WebOrderSystem
public class CallCenterOrderSystem {

    private static final Logger LOG = LoggerFactory.getLogger(CallCenterOrderSystem.class);

    private static final String[] firstNames = {"Alice", "Bob", "Charlie", "Diana"};
    private static final String[] lastNames = {"Smith", "Jones", "Taylor", "Brown"};
    private static final String directoryPath = "src/main/java/de/local/simulate/address/jms/orders/";

    public static void main(String[] args) throws Exception {
        // Ensure the orders directory exists
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                generateOrderFile();
                LOG.info("Generating order file");
            }
        }, 0, 120000); // Every 2 minutes

        CamelContext context = new DefaultCamelContext();

        // Create and configure the ActiveMQ connection factory
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");

        // JMS component configuration
        JmsComponent jmsComponent = JmsComponent.jmsComponent(connectionFactory);
        context.addComponent("jms", jmsComponent);
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("file:" + directoryPath + "?noop=true&delay=120000")
                        .split()
                        .tokenize("\n")
                        .process(exchange -> {
                            LOG.info("Sending order: " + exchange.getMessage().getBody());
                        })
                        .to("jms:queue:incomingOrders");
            }
        });
        context.start();
        Thread.sleep(600000); // Run for 10 minutes
        context.stop();
    }

    private static void generateOrderFile() {
        Random random = new Random();
        StringBuilder orderData = new StringBuilder();

        // customer generating random from first and last name list, but with unique customerID
        // number of ordered surf boards and diving suits random from 0-4
        for (int i = 0; i < random.nextInt(10) + 1; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            int numberOfSurfboards = random.nextInt(5);
            int numberOfDivingSuits = random.nextInt(5);
            String fullName = firstName + lastName;
            String customerId = String.valueOf(fullName.hashCode());

            orderData.append(customerId)
                    .append(", ")
                    .append(firstName)
                    .append(" ")
                    .append(lastName)
                    .append(", ")
                    .append(numberOfSurfboards)
                    .append(", ")
                    .append(numberOfDivingSuits)
                    .append("\n");
        }
        CompletableFuture.runAsync(() -> {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(directoryPath + "order_" + System.currentTimeMillis() + ".txt"))) {
            writer.write(orderData.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }});

    }
}
