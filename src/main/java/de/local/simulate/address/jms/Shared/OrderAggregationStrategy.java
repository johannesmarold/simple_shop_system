package de.local.simulate.address.jms.Shared;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;


public class OrderAggregationStrategy implements AggregationStrategy {
    @Override
    public Exchange aggregate(Exchange firstExchange, Exchange secondExchange) {

        if (firstExchange == null) {
            return secondExchange;
        }
        Order firstOrder = firstExchange.getIn().getBody(Order.class);
        Order secondOrder = secondExchange.getIn().getBody(Order.class);
        if (Boolean.parseBoolean(firstOrder.getValid()) && Boolean.parseBoolean(secondOrder.getValid())) {
            firstOrder.setValidationResult("true");
        }
        else {
            firstOrder.setValidationResult("false");
        }
        firstExchange.getIn().setBody(firstOrder);
        return firstExchange;        }
}
