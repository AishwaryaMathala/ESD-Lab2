
package com.esd.lab2.dto;

import jakarta.validation.constraints.*;

public record SignupRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 64,
              message = "Password must be between 8 and 64 characters")
        @Pattern(regexp = ".*\\d.*",
                 message = "Password must contain at least one digit")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 100,
              message = "Full name must be at most 100 characters")
        String fullName
) {}
