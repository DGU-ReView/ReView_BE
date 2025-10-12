package com.dgu.review.domain.community.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DomainDropdownResponse {
    private final String value; // enum 실제 값 (예: IT_ENGINEERING)
    private final String label; // 표시용 이름 (예: IT, 공학)
}