package tp.choreography.transport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TransportListener {

    @Autowired
    private JmsTemplate jms;

    @Autowired
    private TransportEventAdapter.TransportServiceImpl transport;

    @JmsListener(destination = "PaymentDecided")
    public void onPaymentDecided(String json) {
        try {
            Event e = Event.parse(json);
            boolean approved = e.data.has("approved") && e.data.get("approved").asBoolean();

            if (!approved) {
                return;
            }

            String studentId = e.data.get("studentId").asText();
            System.out.println("[TransportAdapter] Payment approved for studentId=" + studentId + ". Activating pass...");

            TransportEventAdapter.ActivateResponse res = transport.activatePass(new TransportEventAdapter.ActivateRequest(studentId));
            System.out.println("[TransportAdapter] Activated transit pass " + res.getPassId() + " for student " + studentId);

            Map<String, Object> outData = new HashMap<>();
            outData.put("studentId", studentId);
            outData.put("passId", res.getPassId());

            Event out = Event.of("PassActivated", e.correlationId, "transport-service", outData);
            System.out.println("[TransportAdapter] Emitting PassActivated event on topic...");
            jms.convertAndSend("PassActivated", out.toJson());
        } catch (Exception ex) {
            System.err.println("[TransportAdapter] Error handling PaymentDecided: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

