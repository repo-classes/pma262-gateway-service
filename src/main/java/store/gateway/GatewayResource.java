package store.gateway;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayResource {

    @GetMapping("/health-check")
    public ResponseEntity<Map<String, String>> healthCheck() {
        // only the container's hostname, to tell which replica answered:
        // System.getenv() would publish every secret on an open endpoint
        return ResponseEntity.ok(Map.of(
            "hostname", System.getenv().getOrDefault("HOSTNAME", "unknown")
        ));
    }
    
}
