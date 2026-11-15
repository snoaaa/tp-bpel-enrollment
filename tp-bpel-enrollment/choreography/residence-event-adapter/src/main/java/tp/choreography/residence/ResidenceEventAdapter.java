package tp.choreography.residence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;

import java.util.concurrent.ThreadLocalRandom;

@SpringBootApplication
@EnableJms
public class ResidenceEventAdapter {

    public static void main(String[] args) {
        SpringApplication.run(ResidenceEventAdapter.class, args);
    }

    @Bean
    public ResidenceServiceImpl residenceService() {
        return new ResidenceServiceImpl();
    }

    public static class ResidenceServiceImpl {
        public AssignResponse assignRoom(AssignRequest req) {
            int room = ThreadLocalRandom.current().nextInt(101, 450);
            return new AssignResponse("ROOM-" + room);
        }
    }

    public static class AssignRequest {
        public String studentId;
        public AssignRequest(String studentId) {
            this.studentId = studentId;
        }
    }

    public static class AssignResponse {
        private final String roomNumber;
        public AssignResponse(String roomNumber) {
            this.roomNumber = roomNumber;
        }
        public String getRoomNumber() {
            return roomNumber;
        }
    }
}

