package uk.nhs.hee.tis.revalidation.messages.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

public abstract class RabbitMqMessagePublisher<T> implements MessagePublisher<T> {

  protected final String exchange;
  protected final String routingKey;
  protected final RabbitTemplate rabbitTemplate;

  public RabbitMqMessagePublisher(String exchange, String routingKey, RabbitTemplate rabbitTemplate) {
    this.exchange = exchange;
    this.routingKey = routingKey;
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  public void publishToBroker(T message) {
    rabbitTemplate.convertAndSend(exchange, routingKey, message);
  }
}
