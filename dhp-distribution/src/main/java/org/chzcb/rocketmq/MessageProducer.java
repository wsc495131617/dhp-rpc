package org.chzcb.rocketmq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;

@Slf4j
public class MessageProducer {

    private DefaultMQProducer producer;

    public static MessageProducer create(String nameServers, String groupName) {
        MessageProducer messageProducer = new MessageProducer(nameServers, groupName);
        return messageProducer;
    }

    public MessageProducer(String nameServers, String groupName) {
        producer = new DefaultMQProducer(groupName);
        producer.setNamesrvAddr(nameServers);
    }

    public void connect() throws MQClientException {
        producer.start();
    }

    public boolean send(String topic, byte[] body) {
        Message message = new Message(topic, body);
        try {
            SendResult sendResult = producer.send(message);
            if (sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
                return true;
            }
        } catch (Exception e) {
            log.error("MQProducer[] send error: {}", topic, e.getMessage(), e);
        }
        return false;
    }
}
