package tp.choreography.university;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UniversityListener {

    @Autowired
    private JmsTemplate jms;

    @Autowired
    private UniversityEventAdapter.UniversityServiceImpl university;

    private final Map<String, Map<String, Object>> aggregationMap = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingFutures = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> completedResults = new ConcurrentHashMap<>();

    public void registerFuture(String correlationId, CompletableFuture<Map<String, Object>> future) {
        pendingFutures.put(correlationId, future);
    }

    public Map<String, Object> getResult(String correlationId) {
        return completedResults.get(correlationId);
    }

    public Map<String, Map<String, Object>> getAllResults() {
        return completedResults;
    }

    @JmsListener(destination = "EnrollmentRequested")
    public void onEnrollmentRequested(String json) {
        try {
            Event e = Event.parse(json);
            System.out.println("[UniversityAdapter] Received EnrollmentRequested: correlationId=" + e.correlationId);

            String studentInfo = "Anonymous";
            if (e.data.has("student")) {
                com.fasterxml.jackson.databind.JsonNode sNode = e.data.get("student");
                if (sNode.isObject() && sNode.has("name")) {
                    studentInfo = sNode.get("name").asText();
                } else {
                    studentInfo = sNode.asText();
                }
            }
            String amount = e.data.has("amount") ? e.data.get("amount").asText() : "0";

            UniversityEventAdapter.RegisterResponse res = university.register(
                    new UniversityEventAdapter.RegisterRequest(studentInfo, null)
            );

            Map<String, Object> outData = new HashMap<>();
            outData.put("studentId", res.studentId);
            outData.put("amount", amount);
            outData.put("student", studentInfo);

            Event out = Event.of("StudentRegistered", e.correlationId, "university-service", outData);
            System.out.println("[UniversityAdapter] Emitting StudentRegistered for studentId=" + res.studentId);
            jms.convertAndSend("StudentRegistered", out.toJson());
        } catch (Exception ex) {
            System.err.println("[UniversityAdapter] Error handling EnrollmentRequested: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @JmsListener(destination = "PaymentDecided")
    public void onPaymentDecided(String json) {
        try {
            Event e = Event.parse(json);
            boolean approved = e.data.has("approved") && e.data.get("approved").asBoolean();
            String studentId = e.data.has("studentId") ? e.data.get("studentId").asText() : "UNKNOWN";
            String receipt = e.data.has("receipt") ? e.data.get("receipt").asText() : "N/A";

            if (!approved) {
                System.out.println("[UniversityAdapter] Payment refused for correlationId=" + e.correlationId + ". Starting compensation...");
                university.cancel(new UniversityEventAdapter.RegisterRequest(null, studentId));

                Map<String, Object> completedData = new HashMap<>();
                completedData.put("correlationId", e.correlationId);
                completedData.put("studentId", studentId);
                completedData.put("status", "REFUSED");
                completedData.put("reason", "Payment declined by Bank (amount exceeded credit limit of 500,000 FCFA)");
                completedData.put("compensation", "Registration rolled back at University for " + studentId);

                Event completed = Event.of("EnrollmentCompleted", e.correlationId, "university-service", completedData);
                System.out.println("[UniversityAdapter] Emitting EnrollmentCompleted (REFUSED) for " + studentId);
                jms.convertAndSend("EnrollmentCompleted", completed.toJson());

                notifyCompletion(e.correlationId, completedData);
            } else {
                aggregationMap.computeIfAbsent(e.correlationId, k -> new ConcurrentHashMap<>()).put("receipt", receipt);
                aggregationMap.get(e.correlationId).put("studentId", studentId);
                checkCompletion(e.correlationId);
            }
        } catch (Exception ex) {
            System.err.println("[UniversityAdapter] Error in onPaymentDecided: " + ex.getMessage());
        }
    }

    @JmsListener(destination = "RoomAssigned")
    public void onRoomAssigned(String json) {
        try {
            Event e = Event.parse(json);
            String roomNumber = e.data.has("roomNumber") ? e.data.get("roomNumber").asText() : "N/A";
            System.out.println("[UniversityAdapter/Aggregator] Recorded RoomAssigned: " + roomNumber + " for " + e.correlationId);
            aggregationMap.computeIfAbsent(e.correlationId, k -> new ConcurrentHashMap<>()).put("roomNumber", roomNumber);
            checkCompletion(e.correlationId);
        } catch (Exception ex) {
            System.err.println("[UniversityAdapter] Error in onRoomAssigned: " + ex.getMessage());
        }
    }

    @JmsListener(destination = "PassActivated")
    public void onPassActivated(String json) {
        try {
            Event e = Event.parse(json);
            String passId = e.data.has("passId") ? e.data.get("passId").asText() : "N/A";
            System.out.println("[UniversityAdapter/Aggregator] Recorded PassActivated: " + passId + " for " + e.correlationId);
            aggregationMap.computeIfAbsent(e.correlationId, k -> new ConcurrentHashMap<>()).put("passId", passId);
            checkCompletion(e.correlationId);
        } catch (Exception ex) {
            System.err.println("[UniversityAdapter] Error in onPassActivated: " + ex.getMessage());
        }
    }

    private synchronized void checkCompletion(String correlationId) {
        Map<String, Object> state = aggregationMap.get(correlationId);
        if (state != null && state.containsKey("roomNumber") && state.containsKey("passId") && state.containsKey("receipt")) {
            Map<String, Object> completedData = new HashMap<>();
            completedData.put("correlationId", correlationId);
            completedData.put("studentId", state.get("studentId"));
            completedData.put("status", "ENROLLED");
            completedData.put("roomNumber", state.get("roomNumber"));
            completedData.put("passId", state.get("passId"));
            completedData.put("receipt", state.get("receipt"));

            Event completed = Event.of("EnrollmentCompleted", correlationId, "completion-aggregator", completedData);
            System.out.println("[UniversityAdapter/Aggregator] All steps ready! Emitting EnrollmentCompleted (SUCCESS) for " + state.get("studentId"));
            jms.convertAndSend("EnrollmentCompleted", completed.toJson());
            aggregationMap.remove(correlationId);

            notifyCompletion(correlationId, completedData);
        }
    }

    private void notifyCompletion(String correlationId, Map<String, Object> data) {
        completedResults.put(correlationId, data);
        CompletableFuture<Map<String, Object>> future = pendingFutures.remove(correlationId);
        if (future != null) {
            future.complete(data);
        }
    }
}

