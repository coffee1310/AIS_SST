package entity;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api/auth")
public class AuthorizationController {

    @PostMapping
    public ResponseEntity<?> registration() {
        return new ResponseEntity<>("", HttpStatusCode.valueOf(200));
    }
}
