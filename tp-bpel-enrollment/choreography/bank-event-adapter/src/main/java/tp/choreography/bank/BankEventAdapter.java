package tp.choreography.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;

import java.math.BigDecimal;
import java.util.UUID;

@SpringBootApplication
@EnableJms
public class BankEventAdapter {

    public static void main(String[] a) {
        SpringApplication.run(BankEventAdapter.class, a);
    }

    @Bean
    public BankServiceImpl bankService() {
        return new BankServiceImpl();
    }

    public static class BankServiceImpl {
        public PayResponse pay(PayRequest req) {
            boolean ok = req.amount.compareTo(new BigDecimal("500000")) <= 0;
            return new PayResponse(ok, ok ? "RCT-" + UUID.randomUUID().toString().substring(0, 8) : "N/A");
        }
    }

    public static class PayRequest {
        public String studentId;
        public BigDecimal amount;
        public PayRequest(String studentId, BigDecimal amount) {
            this.studentId = studentId;
            this.amount = amount;
        }
    }

    public static class PayResponse {
        private final boolean approved;
        private final String receipt;

        public PayResponse(boolean approved, String receipt) {
            this.approved = approved;
            this.receipt = receipt;
        }

        public boolean isApproved() {
            return approved;
        }

        public String getReceipt() {
            return receipt;
        }
    }
}

