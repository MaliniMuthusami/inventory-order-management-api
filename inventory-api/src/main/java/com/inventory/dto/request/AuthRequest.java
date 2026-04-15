package com.inventory.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequest {

    public static class Register {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        public String getUsername()              { return username; }
        public void setUsername(String u)        { this.username = u; }
        public String getEmail()                 { return email; }
        public void setEmail(String e)           { this.email = e; }
        public String getPassword()              { return password; }
        public void setPassword(String p)        { this.password = p; }
    }

    public static class Login {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;

        public String getUsername()              { return username; }
        public void setUsername(String u)        { this.username = u; }
        public String getPassword()              { return password; }
        public void setPassword(String p)        { this.password = p; }
    }
}
