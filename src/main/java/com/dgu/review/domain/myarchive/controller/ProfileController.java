package com.dgu.review.domain.myarchive.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dgu.review.domain.myarchive.dto.MyProfileGetResponse;
import com.dgu.review.domain.myarchive.dto.MyProfileUpdateRequest;
import com.dgu.review.domain.myarchive.dto.MyProfileUpdateResponse;
import com.dgu.review.domain.myarchive.service.MyProfileService;
import com.dgu.review.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/myarchive/myprofile")
public class ProfileController {

    private final MyProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<MyProfileGetResponse>> getMyTags() {
    	MyProfileGetResponse res = profileService.getProfileTags();
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<MyProfileUpdateResponse>> updateMyTags(
            @RequestBody MyProfileUpdateRequest request
    ) {
    	MyProfileUpdateResponse res = profileService.updateProfileTags(request);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}