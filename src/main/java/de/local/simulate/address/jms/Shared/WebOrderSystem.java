package de.local.simulate.address.jms.Shared;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

// PATTERN DESCRIPTION
// WebOrderSystem acts as a Message Endpoint, generating and sending orders as messages to a JMS queue (incomingOrders)
public class WebOrderSystem {

    private static final Logger LOG = LoggerFactory.getLogger(WebOrderSystem.class);

    public static void main(String[] args) throws Exception {
        CamelContext context = new DefaultCamelContext();

        // Create and configure the ActiveMQ connection factory
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        connectionFactory.setTrustAllPackages(true);
        // JMS component configuration
        JmsComponent jmsComponent = JmsComponent.jmsComponent(connectionFactory);
        context.addComponent("jms", jmsComponent);

        // Scanner for user input
        Scanner scanner = new Scanner(System.in);

        // Define route to send messages to the queue
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("timer://orderTimer?period=10000") // Trigger every 10 seconds (adjust period as needed)
                        .process(exchange -> {
                            // Prompt for order details
                            System.out.println("Enter order details in the format: FirstName,LastName,NumberOfSurfboards,NumberOfDivingSuits,CustomerID");
                            String orderDetails = scanner.nextLine();

                            // Log and send order message
                            LOG.info("Sending order: " + orderDetails);
                            exchange.getIn().setBody(orderDetails);
                        })
                        .to("jms:queue:incomingOrders");
            }
        });

        context.start();
        Thread.sleep(100000000); // Run for 1 minute (adjust as needed)
        context.stop();
        scanner.close(); // Close scanner when done
    }
}
