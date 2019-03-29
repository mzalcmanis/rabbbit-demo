package hello.json;

import lombok.AllArgsConstructor;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class ApplicationJson {

    static final String topicExchangeName = "hello-json-exchange";

    static final String queueName = "hello-json-1";

    static final String queueName2 = "hello-json-2";

    @Bean
    Queue queue() {
        return new Queue(queueName, false);
    }

    @Bean
    Queue queue2() {
        return new Queue(queueName2, false);
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(topicExchangeName);
    }

    @Bean
    Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("foo.bar.#");
    }

    @Bean
    Binding binding2(Queue queue2, TopicExchange exchange) {
        return BindingBuilder.bind(queue2).to(exchange).with("foo.bar.#");
    }


    @Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);

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

    public void receiveMessage(Foo message) {
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
                "foo.bar.baz",
                new Foo("Hello from RabbitMQ!")
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


