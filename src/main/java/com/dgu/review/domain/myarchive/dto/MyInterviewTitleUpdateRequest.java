package com.dgu.review.domain.myarchive.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;


public record MyInterviewTitleUpdateRequest(
	    @NotBlank
	    @Size(max = 20)
	    @JsonProperty("title")
	    String title
	) {}