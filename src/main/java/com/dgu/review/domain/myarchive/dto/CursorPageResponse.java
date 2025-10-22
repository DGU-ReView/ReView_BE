package com.dgu.review.domain.myarchive.dto;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> items,
        Long nextCursor,     // 다음 페이지 조회용 커서, 더 없으면 null
        boolean hasNext      // 다음 페이지가 더 있는지 여부
) { }