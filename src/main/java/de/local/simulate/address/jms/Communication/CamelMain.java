package de.local.simulate.address.jms.Communication;

import de.local.simulate.address.jms.Shared.Order;
import de.local.simulate.address.jms.Shared.OrderAggregationStrategy;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class CamelMain {
    private static final Logger LOG = LoggerFactory.getLogger(CamelMain.class);
    private static final String logFilePath = createLogFilePath("logs/CamelMain");

    public static void main(String[] args) throws Exception {

        createLogFile(logFilePath);

        CamelContext ctxt = new DefaultCamelContext();

        // Create and configure the ActiveMQ connection factory
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        connectionFactory.setTrustAllPackages(true);

        // JMS component configuration
        JmsComponent jmsComponent = JmsComponent.jmsComponent(connectionFactory);
        ctxt.addComponent("jms", jmsComponent);

        RouteBuilder route = new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // PATTERN DESCRIPTION
                // incomingOrders queue is a Point-to-Point Channel that ensures that each order message is consumed
                // by only one receiver. Camel routes the messages from this queue to the processing routes.
                from("jms:queue:incomingOrders")
                        .process(exchange -> {
                            String body = exchange.getIn().getBody(String.class);
                            String[] parts = body.split(",");
                            Order order;
                            // PATTERN DESCRIPTION
                            // The CamelMain processes incoming messages from incomingOrders and translates them
                            // into Order objects. This translation normalizes the input format regardless of
                            // whether the order originated from WebOrderSystem or CallCenterOrderSystem
                            // PATTERN DESCRIPTION
                            // During the processing, additional properties such as OrderID are generated
                            // and enriched the message object before further processing.
                            if (parts.length == 5) {
                                logAndWriteToFile("Received order from WebOrderSystem: " + body);
                                // WebOrderSystem format: FirstName, LastName, NumberOfSurfboards, NumberOfDivingSuits, CustomerID
                                order = new Order(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim(), parts[4].trim());
                            }
                            else if (parts.length == 4){
                                logAndWriteToFile("Received order from CallCenterSystem: " + body);
                                // CallCenterOrderSystem format: CustomerID, Full Name, NumberOfSurfboards, NumberOfDivingSuits
                                String[] nameParts = parts[1].trim().split(" ");
                                order = new Order(nameParts[0], nameParts[1], parts[2].trim(), parts[3].trim(), parts[0].trim());
                            } else {
                                logAndWriteToFile("Received invalid order format " + body);
                                exchange.setProperty("invalidOrder", true);
                                return;
                            }
                            logAndWriteToFile(order + " overall items: " + order.getOverallItems());
                            if (Integer.parseInt(order.getOverallItems()) == 0){
                                logAndWriteToFile("Order doesn' actually order anything " + body);
                                exchange.setProperty("invalidOrder", true);
                                return;
                            }
                            logAndWriteToFile("Forwarding order to BillingSystem and InventorySystem: {}" + order);
                            exchange.getIn().setHeader("JMSReplyTo", "ValidationOut");
                            exchange.getIn().setHeader("JMSCorrelationID", exchange.getIn().getMessageId());
                            exchange.getIn().setBody(order);
                        })
                        // PATTERN DESCRIPTION
                        // After translating and enriching the Order message a Publish-Subscribe Channel is used
                        // where the order is published to multiple subscribers (BillingSystem and InventorySystem).
                        .choice()
                        .when(header("invalidOrder").isNull())
                        .to("jms:topic:ValidationIn")
                        .otherwise()
                        .log("invalid order, not forwarding to ValidationIn")
                        .end();

                        // PATTERN DESCRIPTION
                        // CamelMain aggregates responses from both the BillingSystem and InventorySystem to make
                        // a final decision on the order's validity.
                        // The aggregate EIP collects the responses and only proceeds when both systems have responded.
                        // Aggregate validation results
                        from("jms:queue:validationResult")
                        //.aggregate(header("JMSCorrelationID"), new OrderAggregationStrategy())
                        .aggregate(simple("${body.orderId}"), new OrderAggregationStrategy())
                        .completionSize(2)
                        .process(exchange -> {
                            Order aggregatedOrder = exchange.getIn().getBody(Order.class);
                            logAndWriteToFile("Aggregated Order: " + aggregatedOrder);
                        })

                        // PATTERN DESCRIPTION
                        // After aggregation, a Content-Based Router checks the validity of the order.
                        // Based on the validation result, the order is routed to topic
                        // for either valid orders (validOrders) or invalid orders (invalidOrders).
                        .choice()
                        .when(simple("${body.validationResult} == 'true'"))
                        .to("jms:topic:validOrders")
                        .otherwise()
                        .to("jms:topic:invalidOrders");
            }
        };
        ctxt.addRoutes(route);
        ctxt.start();
        System.in.read();
        ctxt.stop();
    }

    private static String createLogFilePath(String baseFilePath) {
        Random random = new Random();
        long randomNumber = 1000000000L + random.nextLong(9000000000L);
        return baseFilePath + randomNumber + ".log";
    }

    private static void createLogFile(String filePath) {
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            file.createNewFile();
        } catch (IOException e) {
            LOG.error("Failed to create log file", e);
        }
    }

    private static void logAndWriteToFile(String message) {
        LOG.info(message);
        writeToLogFile(message);
    }

    private static void writeToLogFile(String message) {
        try (FileWriter fileWriter = new FileWriter(logFilePath, true);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {
            printWriter.println(message);
        } catch (IOException e) {
            LOG.error("Failed to write log file", e);
        }
    }

}
