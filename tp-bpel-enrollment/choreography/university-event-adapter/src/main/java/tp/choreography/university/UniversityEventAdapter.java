package tp.choreography.university;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableJms
public class UniversityEventAdapter {

    public static void main(String[] args) {
        SpringApplication.run(UniversityEventAdapter.class, args);
    }

    @Bean
    public UniversityServiceImpl universityService() {
        return new UniversityServiceImpl();
    }

    @RestController
    @RequestMapping("/api/enrollments")
    public static class EnrollmentController {

        @Autowired
        private JmsTemplate jms;

        @Autowired
        private UniversityListener listener;

        @PostMapping
        public ResponseEntity<?> requestEnrollment(@RequestBody(required = false) Map<String, Object> body) {
            String correlationId = UUID.randomUUID().toString();
            String student = (body != null && body.containsKey("student")) ? body.get("student").toString() : "Abraham Noumsi";
            String amount = (body != null && body.containsKey("amount")) ? body.get("amount").toString() : "350000";

            Map<String, Object> reqData = new HashMap<>();
            reqData.put("student", student);
            reqData.put("amount", amount);

            CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
            listener.registerFuture(correlationId, future);

            Event event = Event.of("EnrollmentRequested", correlationId, "http-client", reqData);
            System.out.println("[EnrollmentController] Initiating choreography request for: " + student + ", amount: " + amount + " [correlationId=" + correlationId + "]");
            jms.convertAndSend("EnrollmentRequested", event.toJson());

            try {
                Map<String, Object> result = future.get(3500, TimeUnit.MILLISECONDS);
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("correlationId", correlationId);
                fallback.put("status", "PROCESSING");
                fallback.put("message", "Request submitted to Artemis topic EnrollmentRequested. Check GET /api/enrollments/" + correlationId);
                return ResponseEntity.accepted().body(fallback);
            }
        }

        @GetMapping("/{correlationId}")
        public ResponseEntity<?> getStatus(@PathVariable String correlationId) {
            Map<String, Object> res = listener.getResult(correlationId);
            if (res != null) {
                return ResponseEntity.ok(res);
            }
            return ResponseEntity.ok(Map.of(
                    "correlationId", correlationId,
                    "status", "PROCESSING",
                    "message", "Still awaiting steps in choreography pipeline"
            ));
        }

        @GetMapping
        public ResponseEntity<?> getAll() {
            return ResponseEntity.ok(listener.getAllResults());
        }
    }

    public static class UniversityServiceImpl {
        public RegisterResponse register(RegisterRequest req) {
            int randomId = ThreadLocalRandom.current().nextInt(10000, 99999);
            String id = "STU-" + randomId;
            System.out.println("[UniversityAdapter] Registered student " + id + " (" + req.student + ")");
            return new RegisterResponse(id, "ENROLLED");
        }

        public void cancel(RegisterRequest req) {
            System.out.println("[UniversityAdapter] [COMPENSATION] Cancelled registration for student: " + req.studentId);
        }
    }

    public static class RegisterRequest {
        public String student;
        public String studentId;
        public RegisterRequest(String student, String studentId) {
            this.student = student;
            this.studentId = studentId;
        }
    }

    public static class RegisterResponse {
        public String studentId;
        public String status;
        public RegisterResponse(String studentId, String status) {
            this.studentId = studentId;
            this.status = status;
        }
    }
}

