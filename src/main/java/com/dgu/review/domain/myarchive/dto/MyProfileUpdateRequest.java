package com.dgu.review.domain.myarchive.dto;

import java.util.Set;

import com.dgu.review.domain.user.entity.ExperienceTag;
import com.dgu.review.domain.user.entity.GrowthTag;

import lombok.Getter;

@Getter
public class MyProfileUpdateRequest {
    private Set<ExperienceTag> experienceTags;
    private Set<GrowthTag> growthTags;
}

