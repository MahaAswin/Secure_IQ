package com.secureiq.SecureIQ.user.dto;

import com.secureiq.SecureIQ.user.model.Role;
import com.secureiq.SecureIQ.user.model.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPatchRequest {

    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    private Role role;

    private Status status;

    private String profileImage;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
