# Usage

* Produce the payment event
```
 curl -X POST -H "Content-Type: application/json" -d '{"customer":"1","accountFrom":"PARX14107588591","accountTo":"PARX29445321799","amount":100.00,"currency":"EUR"}' http://localhost:8080/payment
```
* The message should land here: http://mayer.kotov.lv:15672/#/queues/%2F/payment.q
* Consume it 

