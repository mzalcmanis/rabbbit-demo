package hello.json;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * {
 * customer: 1,
 * accountFrom: IBAN,
 * accountTo: IBAN,
 * amount: 100,
 * currency: EUR
 * }
 */
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
