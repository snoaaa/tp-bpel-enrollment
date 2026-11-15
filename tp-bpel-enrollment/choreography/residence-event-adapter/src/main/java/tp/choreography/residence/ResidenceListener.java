package tp.choreography.residence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ResidenceListener {

    @Autowired
    private JmsTemplate jms;

    @Autowired
    private ResidenceEventAdapter.ResidenceServiceImpl residence;

    @JmsListener(destination = "PaymentDecided")
    public void onPaymentDecided(String json) {
        try {
            Event e = Event.parse(json);
            boolean approved = e.data.has("approved") && e.data.get("approved").asBoolean();

            if (!approved) {
                return;
            }

            String studentId = e.data.get("studentId").asText();
            System.out.println("[ResidenceAdapter] Payment approved for studentId=" + studentId + ". Allocating room...");

            ResidenceEventAdapter.AssignResponse res = residence.assignRoom(new ResidenceEventAdapter.AssignRequest(studentId));
            System.out.println("[ResidenceAdapter] Allocated room " + res.getRoomNumber() + " for student " + studentId);

            Map<String, Object> outData = new HashMap<>();
            outData.put("studentId", studentId);
            outData.put("roomNumber", res.getRoomNumber());

            Event out = Event.of("RoomAssigned", e.correlationId, "residence-service", outData);
            System.out.println("[ResidenceAdapter] Emitting RoomAssigned event on topic...");
            jms.convertAndSend("RoomAssigned", out.toJson());
        } catch (Exception ex) {
            System.err.println("[ResidenceAdapter] Error handling PaymentDecided: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

