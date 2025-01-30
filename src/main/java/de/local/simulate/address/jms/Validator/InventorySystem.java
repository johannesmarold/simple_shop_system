package de.local.simulate.address.jms.Validator;
import de.local.simulate.address.jms.Shared.Order;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.jms.Queue;
import java.io.IOException;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


// PATTERN DESCRIPTION
// Message Endpoint
// Point-to-Point Channel is represented by JMS queues in this application.
// Each system listens to its specific queue and processes messages sent to that queue/topic
// incoming queue: inventoryValidationIn
// outgoing queue: inventoryValidationOut
public class InventorySystem {

    private static final Logger LOG = LoggerFactory.getLogger(InventorySystem.class);
    static final HashMap<String, Order> validatedOrders = new HashMap<>();
    private static final Timer timer = new Timer();
    private static final long  timeout = 10_000;
    private static final int[] numSurfboards = {75};
    static final int[] numDivingSuits = {90};
    static HashSet<String> timedOutOrders = new HashSet<>();
    static final Lock lock = new ReentrantLock();

    public static void addEntry(Order order){
        lock.lock();
        validatedOrders.put(order.getOrderId(), order);
        lock.unlock();
        timer.schedule(new TimerTask() {
            @Override
            public void run(){
                removeVal(order.getOrderId(), true);
            }
        }, timeout);
    }

    public static void removeVal(String OrderId, boolean fromTimeout){
        lock.lock();
        if (validatedOrders.containsKey(OrderId)) {
            Order validatedOrder = validatedOrders.get(OrderId);
            numSurfboards[0] += Integer.parseInt(validatedOrder.getNumberOfSurfboards());
            numDivingSuits[0] += Integer.parseInt(validatedOrder.getNumberOfDivingSuits());
            validatedOrders.remove(OrderId);
            if (fromTimeout){
                timedOutOrders.add(OrderId);
                LOG.info("Removed valid order because of Timeout: ");
            }
        }
        lock.unlock();
    }
    public static void main(String[] args) {
        try {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
            connectionFactory.setTrustAllPackages(true);
            Connection con = connectionFactory.createConnection();

            final Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic("ValidationIn");
            Queue outQueue = session.createQueue("validationResult");
            MessageConsumer consumer = session.createConsumer(topic);

;            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        Order order = (Order) ((ObjectMessage) message).getObject();
                        LOG.info("incoming message: " + order);
                        // TODO check whether logic works
                        if (numSurfboards[0] >= Integer.parseInt(order.getNumberOfSurfboards()) &&
                                numDivingSuits[0] >= Integer.parseInt(order.getNumberOfDivingSuits())) {
                            numSurfboards[0] -= Integer.parseInt(order.getNumberOfSurfboards());
                            numDivingSuits[0] -= Integer.parseInt(order.getNumberOfDivingSuits());
                            order.setValid("true");
                            addEntry(order);
                        }
                        else{
                            order.setValid("false");
                            LOG.error("Not enough inventory for order: {}", order);
                        }

                        LOG.info("outgoing message: " + order);
                        ObjectMessage answer = session.createObjectMessage(order);
                        answer.setJMSCorrelationID(message.getJMSCorrelationID());
                        MessageProducer producer = session.createProducer(outQueue);
                        producer.send(answer);
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            // final update of orders after being processed from billing AND inventory system
            Topic validMessageTopic = session.createTopic("validOrders");
            MessageConsumer validMessageConsumer = session.createConsumer(validMessageTopic);
            validMessageConsumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        Order order = (Order) ((ObjectMessage) message).getObject();
                        if (timedOutOrders.contains(order.getOrderId())){
                            LOG.info("Received timed out order: {}", order);
                            timedOutOrders.remove(order.getOrderId());
                            return;
                        }
                        LOG.info("Validated Order: {}", order.getOrderId());
                        lock.lock();
                        validatedOrders.remove(order.getOrderId());
                        lock.unlock();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });
            Topic invalidMessageTopic = session.createTopic("invalidOrders");
            MessageConsumer invalidMessageConsumer = session.createConsumer(invalidMessageTopic);
            invalidMessageConsumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        Order order = (Order) ((ObjectMessage) message).getObject();
                        removeVal(order.getOrderId(), false);
                        }
                     catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            con.start();
            System.in.read();
            con.close();
        } catch (JMSException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
