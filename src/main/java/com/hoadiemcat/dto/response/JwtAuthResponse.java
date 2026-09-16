package com.hoadiemcat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String role;
        private String avatarUrl;
        private java.util.List<String> permissions;
    }
}
