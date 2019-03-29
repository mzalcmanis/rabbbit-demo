package hello.json;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

    static final String TOPIC_EXCHANGE_NAME = "payment-exchange";

    static final String QUEUE_PAYMENTS = "payment.q";

    static final String QUEUE_ALL = "all.event.q";

    @Bean
    Queue queue() {
        return new Queue(QUEUE_PAYMENTS, false);
    }

    @Bean
    Queue queueAllEvents() {
        return new Queue(QUEUE_ALL, false);
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(TOPIC_EXCHANGE_NAME);
    }

    @Bean
    Binding bindingPayments(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("event.payment.#");
    }

    @Bean
    Binding bindingAll(Queue queueAllEvents, TopicExchange exchange) {
        return BindingBuilder.bind(queueAllEvents).to(exchange).with("event.#");
    }

    @Bean
    Jackson2JsonMessageConverter jsonConverter() {
        Jackson2JsonMessageConverter jsonConverter = new Jackson2JsonMessageConverter();
        jsonConverter.setClassMapper(classMapper());
        return jsonConverter;
    }

    @Bean
    DefaultClassMapper classMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("payment", Payment.class);
        classMapper.setIdClassMapping(idClassMapping);
        classMapper.setTrustedPackages("hello.json");
        return classMapper;
    }

    @Bean
    RabbitTemplate jsonRabbitTemplate(Jackson2JsonMessageConverter jsonConverter, ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        rabbitTemplate.setConnectionFactory(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonConverter);
        return rabbitTemplate;
    }
}



