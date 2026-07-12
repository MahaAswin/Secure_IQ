package com.secureiq.SecureIQ.student.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityResponse {
    private String title;
    private String description;
    private String timestamp;
}
