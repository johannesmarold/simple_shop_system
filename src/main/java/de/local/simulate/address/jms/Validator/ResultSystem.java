package de.local.simulate.address.jms.Validator;

import de.local.simulate.address.jms.Shared.Order;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.IOException;

// PATTERN DESCRIPTION
// Message Endpoint
// Point-to-Point Channel is represented by JMS queues in this application.
// Each system listens to its specific queue and processes messages sent to that queue
// incoming queues: validOrders, invalidOrders
public class ResultSystem {

    private static final Logger LOG = LoggerFactory.getLogger(ResultSystem.class);

    public static void main(String[] args) {
        try {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            connectionFactory.setTrustAllPackages(true);
            Connection con = connectionFactory.createConnection();

            final Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic validQueue = session.createTopic("validOrders");
            Topic invalidQueue = session.createTopic("invalidOrders");
            MessageConsumer validConsumer = session.createConsumer(validQueue);
            MessageConsumer invalidConsumer = session.createConsumer(invalidQueue);

            validConsumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        if (message instanceof ObjectMessage) {
                            ObjectMessage objMessage = (ObjectMessage) message;
                            Order order = (Order) objMessage.getObject();
                            // Process the valid order
                            LOG.info("Received valid order: " + order);
                        }
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            invalidConsumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        if (message instanceof ObjectMessage) {
                            ObjectMessage objMessage = (ObjectMessage) message;
                            Order order = (Order) objMessage.getObject();
                            // Process the invalid order
                            LOG.info("Received invalid order: " + order);
                        }
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            con.start();
            System.in.read();
            con.close();
        } catch (JMSException | IOException e) {
            e.printStackTrace();
        }
    }
}

