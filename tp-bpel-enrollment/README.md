# TP 4: Service Composition - WS-BPEL Orchestration & JMS Choreography

**Course:** Information Systems Architecture  
**Academic Year:** 2026-2027  
**Lecturer:** Dr. Emmanuel MOUPOJOU  
**Student:** SIMO NOUMSI Abraham (ID: 2324i242) - INGE 4 ISI EN  

---

## 1. Description

This practical lab implements the student enrollment workflow from Chapter 4 using two different service composition approaches:

* **Part A - Orchestration (WS-BPEL 2.0):** A central BPEL process deployed on Apache ODE calls the 4 partner services (University, Bank, Residence, Transport). It registers the student, checks payment, runs room allocation and bus pass activation in parallel (`<flow>`), and cancels the registration if the bank payment is declined.
* **Part B - Choreography (JMS / ActiveMQ Artemis):** The services talk directly through an event broker without any central engine. Four Spring Boot adapters listen to and publish JSON events on multicast topics.

---

## 2. Project Tree

```
tp-bpel-enrollment/
├── pom.xml
├── README.md
├── enrollment-requests.http
├── services/
│   ├── pom.xml
│   ├── university-service/
│   ├── bank-service/
│   ├── residence-service/
│   └── transport-service/
├── bpel-orchestrator/
│   ├── EnrollmentProcess.bpel
│   ├── EnrollmentProcess.cbp
│   ├── EnrollmentProcess.wsdl
│   ├── bank.wsdl
│   ├── university.wsdl
│   ├── residence.wsdl
│   ├── transport.wsdl
│   ├── deploy.xml
│   └── types/
│       └── enrollment.xsd
├── choreography/
│   ├── pom.xml
│   ├── university-event-adapter/
│   ├── bank-event-adapter/
│   ├── residence-event-adapter/
│   └── transport-event-adapter/
└── soapui/
    ├── enrollment-tests.xml
    └── enrollment-requests.http
```

---

## 3. Required Tools

* **JDK:** Adoptium Temurin 11 (LTS) - required by Apache ODE
* **Build:** Apache Maven 3.9.x
* **Servlet Container:** Apache Tomcat 9.0.x with Apache ODE 1.3.8 deployed in `webapps/ode`
* **Message Broker:** Apache ActiveMQ Artemis 2.33.x
* **IDE:** IntelliJ IDEA (Community Edition or Ultimate)
* **Testing:** IntelliJ HTTP Client, SoapUI 5.7, or `curl`

---

## 4. Setting Up in IntelliJ IDEA (Community & Ultimate)

### 1. Open the Project
1. Open IntelliJ IDEA.
2. Go to **File -> Open...** and select the `tp-bpel-enrollment` folder.
3. Click **Open as Project** and trust the project.

### 2. Set Project JDK to 11
1. Go to **File -> Project Structure** (`Ctrl + Alt + Shift + S`).
2. Under **Project -> SDK**, choose JDK 11 (Adoptium Temurin 11).
3. Set **Language Level** to 11.
4. Click **OK**.

### 3. Build the Project
* **Using Maven Tool Window:**
  Open the Maven tab on the right -> Click Reload -> Go to **tp-bpel-enrollment -> Lifecycle** -> Double-click `clean`, then double-click `install`.
* **Using Terminal:**
  Open the terminal tab (`Alt + F12`) and run:
  ```bash
  mvn clean install -DskipTests
  ```

---

## 5. Running Part A: WS-BPEL Orchestration

### Step 1: Deploy Partner Services to Tomcat 9
Copy the 4 generated WAR files into your Tomcat `webapps/` folder:
* `services/university-service/target/university-service.war`
* `services/bank-service/target/bank-service.war`
* `services/residence-service/target/residence-service.war`
* `services/transport-service/target/transport-service.war`

Check the service WSDLs in your browser:
* University: `http://localhost:8080/university-service/UniversityService?wsdl`
* Bank: `http://localhost:8080/bank-service/BankService?wsdl`
* Residence: `http://localhost:8080/residence-service/ResidenceService?wsdl`
* Transport: `http://localhost:8080/transport-service/TransportService?wsdl`

### Step 2: Deploy the BPEL Process to Apache ODE
Copy the whole `bpel-orchestrator/` folder into `$TOMCAT_HOME/webapps/ode/WEB-INF/processes/enrollment/`.

Check in ODE:
* Open `http://localhost:8080/ode/` -> `EnrollmentProcess` should show status **ACTIVE**.
* WSDL: `http://localhost:8080/ode/processes/EnrollmentService?wsdl`

### Step 3: Start Tomcat 9
In a terminal:
```bash
# Windows
catalina.bat run

# Linux / macOS
./catalina.sh run
```

---

## 6. Running Part B: JMS Event Choreography

### Step 1: Start ActiveMQ Artemis Broker
In a separate terminal:
```bash
# Windows
artemis-broker\bin\artemis.cmd run

# Linux / macOS
~/artemis-broker/bin/artemis run
```
The Artemis console runs at `http://localhost:8161` (user: `admin`, pass: `admin`).

### Step 2: Start the 4 Event Adapters
You can run them in two ways:

* **Method 1 - From IntelliJ (Community & Ultimate):**
  In the project tree, right-click and run `main()` for each adapter:
  1. `choreography/university-event-adapter/.../UniversityEventAdapter.java` -> Run
  2. `choreography/bank-event-adapter/.../BankEventAdapter.java` -> Run
  3. `choreography/residence-event-adapter/.../ResidenceEventAdapter.java` -> Run
  4. `choreography/transport-event-adapter/.../TransportEventAdapter.java` -> Run

* **Method 2 - From Terminal:**
  Open 4 terminal tabs and run:
  ```bash
  mvn spring-boot:run -f choreography/university-event-adapter/pom.xml
  mvn spring-boot:run -f choreography/bank-event-adapter/pom.xml
  mvn spring-boot:run -f choreography/residence-event-adapter/pom.xml
  mvn spring-boot:run -f choreography/transport-event-adapter/pom.xml
  ```

---

## 7. How to Test

### Option 1: IntelliJ HTTP Client (Recommended)
Open `soapui/enrollment-requests.http` directly in IntelliJ and click the green play icon next to each request:
* **Request #2:** Part A Nominal (350,000 FCFA) -> returns `ENROLLED` with room, pass, and receipt.
* **Request #3:** Part A Refusal (900,000 FCFA) -> returns `REFUSED`, Tomcat console shows `[COMPENSATION] Student registration CANCELLED`.
* **Request #4:** Part A Boundary (500,000 FCFA) -> returns `ENROLLED`.
* **Request #5:** Part B Nominal (350,000 FCFA) -> triggers the Artemis event flow and returns `ENROLLED`.
* **Request #6:** Part B Refusal (900,000 FCFA) -> triggers bank payment decline and university registration cancellation.

### Option 2: SoapUI 5.7 GUI
1. Open SoapUI.
2. Go to **File -> Import Project** and select `soapui/enrollment-tests.xml`.
3. Open **TestSuite1** and click the green Play button.
4. All 4 test cases (`TC01_Nominal`, `TC02_Refusal_Compensation`, `TC03_Boundary_500k`, `TC04_Minimal`) will run and pass green.

### Option 3: Terminal / cURL

**Part A (SOAP to ODE on port 8080):**
```bash
# Nominal (350,000 FCFA):
curl -X POST http://localhost:8080/ode/processes/EnrollmentService \
  -H "Content-Type: text/xml;charset=UTF-8" \
  -H "SOAPAction: enroll" \
  -d "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tns=\"http://tp.bpel/enrollment/\"><soapenv:Body><tns:enrollRequest><student>Abraham Noumsi</student><amount>350000</amount></tns:enrollRequest></soapenv:Body></soapenv:Envelope>"

# Refusal (900,000 FCFA):
curl -X POST http://localhost:8080/ode/processes/EnrollmentService \
  -H "Content-Type: text/xml;charset=UTF-8" \
  -H "SOAPAction: enroll" \
  -d "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tns=\"http://tp.bpel/enrollment/\"><soapenv:Body><tns:enrollRequest><student>Abraham Noumsi</student><amount>900000</amount></tns:enrollRequest></soapenv:Body></soapenv:Envelope>"
```

**Part B (REST to University adapter on port 8081):**
```bash
# Nominal (350,000 FCFA):
curl -X POST http://localhost:8081/api/enrollments \
  -H "Content-Type: application/json" \
  -d "{\"student\":\"Abraham Noumsi\",\"amount\":350000}"

# Refusal (900,000 FCFA):
curl -X POST http://localhost:8081/api/enrollments \
  -H "Content-Type: application/json" \
  -d "{\"student\":\"Abraham Noumsi\",\"amount\":900000}"
```
