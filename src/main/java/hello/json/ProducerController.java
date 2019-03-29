package hello.json;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RequestMapping(
        value = "/",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
)
@Slf4j
@RestController
class ProducerController {

    @Autowired
    private RabbitTemplate jsonRabbitTemplate;

    @PostMapping("/payment")
    public ResponseEntity<String> payment(@RequestBody Payment payment) {

        try {
            jsonRabbitTemplate.convertAndSend(
                    RabbitConfig.TOPIC_EXCHANGE_NAME,
                    "event.payment",
                    payment
            );
        } catch (AmqpException e){
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok("Payment event successfully submitted to exchange " + RabbitConfig.TOPIC_EXCHANGE_NAME);
    }

    @PostMapping("/random-payment")
    public ResponseEntity<String> randomPayment() {

        try {
            jsonRabbitTemplate.convertAndSend(
                    RabbitConfig.TOPIC_EXCHANGE_NAME,
                    "event.payment",
                    Payment.builder()
                            .accountFrom("PARX1" + RandomStringUtils.randomNumeric(10))
                            .accountTo("PARX2" + RandomStringUtils.randomNumeric(10))
                            .amount(new BigDecimal(100).setScale(2))
                            .customer("1")
                            .currency("EUR")
                            .build()
            );
        } catch (AmqpException e){
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok("Payment event successfully submitted to exchange " + RabbitConfig.TOPIC_EXCHANGE_NAME);
    }

}
