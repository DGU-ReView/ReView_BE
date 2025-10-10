package com.dgu.review.domain.community.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 면접 domain 카테고리 정의
 */
@Getter
@RequiredArgsConstructor
public enum DomainCategory {

    IT_ENGINEERING("IT, 공학"),
    BUSINESS_FINANCE("경영, 사무, 금융"),
    PUBLIC_SOCIAL("공공, 사회서비스"),
    HEALTH_MEDICAL("의료, 보건"),
    ART_MEDIA("예술, 미디어"),
    SERVICE_TOURISM("서비스, 관광"),
    SALES_DISTRIBUTION("영업, 유통"),
    TECH_MANUFACTURING_CONSTRUCTION("기술, 생산, 건설"),
    AGRICULTURE_FISHERY("농림, 수산");

    private final String displayName;
}
