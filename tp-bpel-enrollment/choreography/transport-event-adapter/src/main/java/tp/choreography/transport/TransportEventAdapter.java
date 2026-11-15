package tp.choreography.transport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;

import java.util.concurrent.ThreadLocalRandom;

@SpringBootApplication
@EnableJms
public class TransportEventAdapter {

    public static void main(String[] args) {
        SpringApplication.run(TransportEventAdapter.class, args);
    }

    @Bean
    public TransportServiceImpl transportService() {
        return new TransportServiceImpl();
    }

    public static class TransportServiceImpl {
        public ActivateResponse activatePass(ActivateRequest req) {
            int pass = ThreadLocalRandom.current().nextInt(1000, 9999);
            return new ActivateResponse("PASS-" + pass);
        }
    }

    public static class ActivateRequest {
        public String studentId;
        public ActivateRequest(String studentId) {
            this.studentId = studentId;
        }
    }

    public static class ActivateResponse {
        private final String passId;
        public ActivateResponse(String passId) {
            this.passId = passId;
        }
        public String getPassId() {
            return passId;
        }
    }
}

