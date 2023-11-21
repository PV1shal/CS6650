package RabbitMQ;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQConnectionFactory {
    private static ConnectionFactory factory;
    private static Connection connection;
    private static String QUEUE_NAME = "HW3";
    private static Channel channel;

    static {
        factory = new ConnectionFactory();
        factory.setHost("ec2-54-190-50-155.us-west-2.compute.amazonaws.com");
        factory.setPort(5672);
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ConnectionFactory getFactory() {
        return factory;
    }
    public static Connection getNewConnection() {
        try {
            return factory.newConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Connection getConnection() {
        return connection;
    }

    public static String getQueueName() {
        return QUEUE_NAME;
    }

    public static Channel getChannel() {
        return channel;
    }
}
