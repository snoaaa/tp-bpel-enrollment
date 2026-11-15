package bpel.tp.transport;

import javax.jws.WebService;
import java.util.concurrent.ThreadLocalRandom;

@WebService(
    serviceName = "TransportService",
    portName = "TransportPort",
    endpointInterface = "bpel.tp.transport.TransportPT",
    targetNamespace = "http://tp.bpel/transport/",
    wsdlLocation = "WEB-INF/wsdl/transport.wsdl"
)
public class TransportServiceImpl implements TransportPT {

    @Override
    public ActivateResponse activatePass(ActivateRequest req) {
        ActivateResponse r = new ActivateResponse();
        int passNum = ThreadLocalRandom.current().nextInt(1000, 9999);
        r.setPassId("PASS-" + passNum);
        String studentId = (req != null && req.getStudentId() != null) ? req.getStudentId() : "UNKNOWN";
        System.out.println("[TransportService] Transit pass " + r.getPassId() + " activated for student: " + studentId);
        return r;
    }
}

