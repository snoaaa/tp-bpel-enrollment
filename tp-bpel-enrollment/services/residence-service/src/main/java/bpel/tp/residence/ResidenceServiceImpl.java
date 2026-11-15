package bpel.tp.residence;

import javax.jws.WebService;
import java.util.concurrent.ThreadLocalRandom;

@WebService(
    serviceName = "ResidenceService",
    portName = "ResidencePort",
    endpointInterface = "bpel.tp.residence.ResidencePT",
    targetNamespace = "http://tp.bpel/residence/",
    wsdlLocation = "WEB-INF/wsdl/residence.wsdl"
)
public class ResidenceServiceImpl implements ResidencePT {

    @Override
    public AssignResponse assignRoom(AssignRequest req) {
        AssignResponse r = new AssignResponse();
        int roomNum = ThreadLocalRandom.current().nextInt(101, 450);
        r.setRoomNumber("ROOM-" + roomNum);
        String studentId = (req != null && req.getStudentId() != null) ? req.getStudentId() : "UNKNOWN";
        System.out.println("[ResidenceService] Room " + r.getRoomNumber() + " allocated to student: " + studentId);
        return r;
    }
}

