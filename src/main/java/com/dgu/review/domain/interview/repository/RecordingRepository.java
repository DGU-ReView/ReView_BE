package com.dgu.review.domain.interview.repository;

import com.dgu.review.domain.interview.entity.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecordingRepository extends JpaRepository<Recording, Long> {

    // setter 없이 sttText만 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Recording r set r.sttText = :text where r.id = :id")
    int updateSttTextById(Long id, String text);
}
