package tp.choreography.residence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class Event {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String eventId;
    public String correlationId;
    public String timestamp;
    public String source;
    public String type;
    public JsonNode data;

    public Event() {}

    public static Event parse(String json) throws Exception {
        JsonNode root = MAPPER.readTree(json);
        Event e = new Event();
        if (root.has("eventId")) e.eventId = root.get("eventId").asText();
        if (root.has("correlationId")) e.correlationId = root.get("correlationId").asText();
        if (root.has("timestamp")) e.timestamp = root.get("timestamp").asText();
        if (root.has("source")) e.source = root.get("source").asText();
        if (root.has("type")) e.type = root.get("type").asText();
        if (root.has("data")) e.data = root.get("data");
        return e;
    }

    public static Event of(String type, String correlationId, String source, Map<String, Object> dataMap) {
        Event e = new Event();
        e.eventId = UUID.randomUUID().toString();
        e.correlationId = correlationId;
        e.timestamp = Instant.now().toString();
        e.source = source;
        e.type = type;
        e.data = MAPPER.valueToTree(dataMap);
        return e;
    }

    public String toJson() {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("eventId", eventId != null ? eventId : UUID.randomUUID().toString());
            root.put("correlationId", correlationId != null ? correlationId : "");
            root.put("timestamp", timestamp != null ? timestamp : Instant.now().toString());
            root.put("source", source != null ? source : "");
            root.put("type", type != null ? type : "");
            if (data != null) {
                root.set("data", data);
            } else {
                root.putObject("data");
            }
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception ex) {
            throw new RuntimeException("Error serializing Event to JSON", ex);
        }
    }

    @Override
    public String toString() {
        return toJson();
    }
}
