package de.local.simulate.address.jms.Shared;

import java.io.Serializable;
import java.util.UUID;

public class Order implements Serializable {
    private String firstName;
    private String lastName;
    private String numberOfSurfboards;
    private String numberOfDivingSuits;
    private String overallItems;
    private String customerId;
    private String orderId;
    private String valid;
    private String validationResult;

    public Order(String firstName, String lastName, String numberOfSurfboards, String numberOfDivingSuits, String customerId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.numberOfSurfboards = numberOfSurfboards;
        this.numberOfDivingSuits = numberOfDivingSuits;
        this.overallItems = String.valueOf(Integer.parseInt(numberOfSurfboards) + Integer.parseInt(numberOfDivingSuits));
        this.customerId = customerId;
        this.orderId = UUID.randomUUID().toString();
        this.valid = "false";
        this.validationResult = "false";
    }

    // Getters and Setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNumberOfSurfboards() {
        return numberOfSurfboards;
    }

    public void setNumberOfSurfboards(String numberOfSurfboards) {
        this.numberOfSurfboards = numberOfSurfboards;
    }

    public String getNumberOfDivingSuits() {
        return numberOfDivingSuits;
    }

    public void setNumberOfDivingSuits(String numberOfDivingSuits) {
        this.numberOfDivingSuits = numberOfDivingSuits;
    }

    public String getOverallItems() { return overallItems; }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getValid() {
        return valid;
    }

    public void setValid(String valid) {
        this.valid = valid;
    }

    public void addToValid(String valid) {
        this.valid.concat(valid);
    }

    public String getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(String validationResult) {
        this.validationResult = validationResult;
    }

    public void addToValidationResult(String validationResult) {
        this.validationResult.concat(validationResult);
    }

    @Override
    public String toString() {
        return "Order{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", numberOfSurfboards=" + numberOfSurfboards +
                ", numberOfDivingSuits=" + numberOfDivingSuits +
                ", overAllItems=" + overallItems +
                ", customerId='" + customerId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", valid=" + valid +
                ", validationResult='" + validationResult + '\'' +
                '}';
    }
}

