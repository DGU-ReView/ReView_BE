package com.dgu.review.domain.myarchive.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dgu.review.domain.myarchive.dto.MyProfileGetResponse;
import com.dgu.review.domain.myarchive.dto.MyProfileUpdateRequest;
import com.dgu.review.domain.myarchive.dto.MyProfileUpdateResponse;
import com.dgu.review.domain.user.entity.ExperienceTag;
import com.dgu.review.domain.user.entity.GrowthTag;
import com.dgu.review.domain.user.entity.User;
import com.dgu.review.domain.user.repository.UserRepository;
import com.dgu.review.domain.user.service.GetUserService;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyProfileService {

    private final UserRepository userRepository;
    private final GetUserService getUserService;

    @Transactional(readOnly = true)
    public MyProfileGetResponse getProfileTags() {
    	Long userId = getUserService.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return MyProfileGetResponse.builder()
                .experienceTags(user.getExperienceTags())
                .growthTags(user.getGrowthTags())
                .build();
    }

    @Transactional
    public MyProfileUpdateResponse updateProfileTags(MyProfileUpdateRequest request) {
    	Long userId = getUserService.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        Set<ExperienceTag> newExp = request.getExperienceTags();
        Set<GrowthTag> newGrow = request.getGrowthTags();

        // null 방어
        if (newExp == null || newGrow == null) {
            throw new ApiException(ErrorCode.INVALID_TAG_REQUEST);
        }

        // 개수 제한: 각 3개까지
        if (newExp.size() > 3 || newGrow.size() > 3) {
            throw new ApiException(ErrorCode.TAG_LIMIT_EXCEEDED);
        }

        // 갈아끼우기 (clear 후 addAll로 교체)
        user.getExperienceTags().clear();
        user.getExperienceTags().addAll(newExp);

        user.getGrowthTags().clear();
        user.getGrowthTags().addAll(newGrow);

        // dirty checking으로 flush/UPDATE 됨

        return MyProfileUpdateResponse.builder()
        		.message("프로필 태그가 저장되었습니다.")
                .experienceTags(user.getExperienceTags())
                .growthTags(user.getGrowthTags())
                .build();
    }
}
