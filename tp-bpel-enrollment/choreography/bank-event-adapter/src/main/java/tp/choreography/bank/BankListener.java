package tp.choreography.bank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class BankListener {

    @Autowired
    private JmsTemplate jms;

    @Autowired
    private BankEventAdapter.BankServiceImpl bank;

    @JmsListener(destination = "StudentRegistered")
    public void onStudentRegistered(String json) {
        try {
            Event e = Event.parse(json);
            System.out.println("[BankAdapter] Received StudentRegistered: correlationId=" + e.correlationId);

            String studentId = e.data.get("studentId").asText();
            String amountStr = e.data.has("amount") ? e.data.get("amount").asText() : "0";
            BigDecimal amount = new BigDecimal(amountStr);

            BankEventAdapter.PayResponse r = bank.pay(new BankEventAdapter.PayRequest(studentId, amount));
            System.out.println("[BankAdapter] Processed payment for student " + studentId + " (amount=" + amount + "): approved=" + r.isApproved() + ", receipt=" + r.getReceipt());

            Map<String, Object> outData = new HashMap<>();
            outData.put("studentId", studentId);
            outData.put("approved", r.isApproved());
            outData.put("receipt", r.getReceipt());
            outData.put("amount", amountStr);

            Event out = Event.of("PaymentDecided", e.correlationId, "bank-service", outData);
            System.out.println("[BankAdapter] Emitting PaymentDecided event on topic...");
            jms.convertAndSend("PaymentDecided", out.toJson());
        } catch (Exception ex) {
            System.err.println("[BankAdapter] Error handling StudentRegistered: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

