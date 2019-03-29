package hello.json;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class ApplicationJson {

    static final String topicExchangeName = "payment-exchange";

    static final String queuePayments = "payment.q";

    static final String queueAll = "all.event.q";

    @Bean
    Queue queue() {
        return new Queue(queuePayments, false);
    }

    @Bean
    Queue queueAllEvents() {
        return new Queue(queueAll, false);
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(topicExchangeName);
    }

    @Bean
    Binding bindingPayments(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("foo.payment.#");
    }

    @Bean
    Binding bindingAll(Queue queueAllEvents, TopicExchange exchange) {
        return BindingBuilder.bind(queueAllEvents).to(exchange).with("foo.#");
    }


    @Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queuePayments);

        Jackson2JsonMessageConverter jsonConverter = jsonConverter();
        listenerAdapter.setMessageConverter(jsonConverter);

        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
    Jackson2JsonMessageConverter jsonConverter() {
        Jackson2JsonMessageConverter jsonConverter = new Jackson2JsonMessageConverter();
        jsonConverter.setClassMapper(classMapper());
        return jsonConverter;
    }

    @Bean
    public DefaultClassMapper classMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("foo", Foo.class);
        idClassMapping.put("payment", Payment.class);
        classMapper.setIdClassMapping(idClassMapping);
        classMapper.setTrustedPackages("hello.json");
        return classMapper;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(Receiver receiver) {
        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(receiver, "receiveMessage");
        return messageListenerAdapter;
    }

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(ApplicationJson.class, args).close();
    }

}

@Component
class Receiver {

    private CountDownLatch latch = new CountDownLatch(1);

    public void receiveMessage(Payment message) {
        System.out.println("Received <" + message + ">");
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

}

@Component
class Runner implements CommandLineRunner {

    private final RabbitTemplate rabbitTemplate;
    private final Receiver receiver;

    public Runner(Receiver receiver, RabbitTemplate rabbitTemplate, Jackson2JsonMessageConverter converter) {
        this.receiver = receiver;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(converter);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Sending message...");
        rabbitTemplate.convertAndSend(
                ApplicationJson.topicExchangeName,
                "foo.payment.wire",
                Payment.builder()
                        .accountFrom("PARX1")
                        .accountTo("PARX2")
                        .amount(new BigDecimal(100).setScale(2))
                        .customer("1")
                        .currency("EUR")
                        .build()
        );

        receiver.getLatch().await(10000, TimeUnit.MILLISECONDS);
    }

}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Foo {
    String message;
}

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
class Payment {
    String customer;
    String accountFrom;
    String accountTo;
    BigDecimal amount;
    String currency;
}

/**
 * {
 * customer: 1,
 * accountFrom: IBAN,
 * accountTo: IBAN,
 * amount: 100,
 * currency: EUR
 * }
 */


