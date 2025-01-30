# Simple Shop System using Apache Camel

This shop system framework integrates multiple systems using Apache Camel for Enterprise Application Integration (EAI). It enables seamless communication between different components using ActiveMQ-based messaging to facilitate Inventory Management and Billing.

## Requirements
Before setting up the system, ensure you have the following installed:

- Java 22 (JDK 22 as per Maven configuration)

- Apache Camel 3.11.0

- Maven (for dependency management)

- ActiveMQ 5.16.3 (for messaging between components)

## Running Application
1. navigate to directory

     ```bash
     cd path/to/simple_shop_system
     ```
   
2. build the project

     ```bash
     mvn clean compile
     ```

3. start each application separately
     ```bash
     mvn exec:java -Dexec.mainClass="de.local.simulate.address.jms.Communication.CamelMain"
     mvn exec:java -Dexec.mainClass="de.local.simulate.address.jms.Shared.WebOrderSystem"
     mvn exec:java -Dexec.mainClass="de.local.simulate.address.jms.Shared.CallCenterOrderSystem"
     mvn exec:java -Dexec.mainClass="de.local.simulate.address.jms.Validator.BillingSystem"
     mvn exec:java -Dexec.mainClass="de.local.simulate.address.jms.Validator.InventorySystem"
     mvn exec:java -Dexec.mainClass="de.local.simulate.address.jms.Validator.ResultSystem"
     ```
4. place an order in the WebOrderSystem by entering in the command line:
     ```bash
     [First Name], [Last Name], [Number of Surfboards], [Number of Diving Suits], [Customer ID]
     ```   