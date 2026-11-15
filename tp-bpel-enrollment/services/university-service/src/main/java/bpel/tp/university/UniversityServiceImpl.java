package bpel.tp.university;

import javax.jws.WebService;
import java.util.concurrent.ThreadLocalRandom;

@WebService(
    serviceName = "UniversityService",
    portName = "UniversityPort",
    endpointInterface = "bpel.tp.university.UniversityPT",
    targetNamespace = "http://tp.bpel/university/",
    wsdlLocation = "WEB-INF/wsdl/university.wsdl"
)
public class UniversityServiceImpl implements UniversityPT {

    @Override
    public RegisterResponse register(RegisterRequest req) {
        RegisterResponse r = new RegisterResponse();
        int randomId = ThreadLocalRandom.current().nextInt(10000, 99999);
        r.setStudentId("STU-" + randomId);
        r.setStatus("ENROLLED");
        System.out.println("[UniversityService] Student registered successfully: " + r.getStudentId() + " (" + (req != null ? req.getStudent() : "") + ")");
        return r;
    }

    @Override
    public void cancel(RegisterRequest req) {
        String studentId = (req != null && req.getStudentId() != null) ? req.getStudentId() : "UNKNOWN";
        System.out.println("[UniversityService] [COMPENSATION] Student registration CANCELLED for: " + studentId);
    }
}

