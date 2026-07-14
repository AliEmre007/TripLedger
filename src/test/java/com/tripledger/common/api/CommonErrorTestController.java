package com.tripledger.common.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/common-error-test")
public class CommonErrorTestController {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    TestResponse accept(@Valid @RequestBody TestRequest request) {
        return new TestResponse(request.name());
    }

    @GetMapping("/failure")
    TestResponse fail() {
        throw new IllegalStateException("sensitive failure detail");
    }

    record TestRequest(@NotBlank String name) {
    }

    record TestResponse(String name) {
    }
}
