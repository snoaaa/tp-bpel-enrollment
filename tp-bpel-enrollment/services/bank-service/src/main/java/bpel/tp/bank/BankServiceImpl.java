package bpel.tp.bank;

import javax.jws.WebService;
import java.math.BigDecimal;
import java.util.UUID;

@WebService(
    serviceName = "BankService",
    portName = "BankPort",
    endpointInterface = "bpel.tp.bank.BankPT",
    targetNamespace = "http://tp.bpel/bank/",
    wsdlLocation = "WEB-INF/wsdl/bank.wsdl"
)
public class BankServiceImpl implements BankPT {

    @Override
    public PayResponse pay(PayRequest req) {
        PayResponse r = new PayResponse();
        boolean ok = req != null && req.getAmount() != null && req.getAmount().compareTo(new BigDecimal("500000")) <= 0;
        r.setApproved(ok);
        r.setReceipt(ok ? "RCT-" + UUID.randomUUID().toString().substring(0, 8) : "N/A");
        return r;
    }
}

