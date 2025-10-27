package com.dgu.review.domain.user.converter;

import java.util.Set;

import com.dgu.review.domain.user.entity.ExperienceTag;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ExperienceTagSetConverter implements AttributeConverter<Set<ExperienceTag>, String> {
    @Override public String convertToDatabaseColumn(Set<ExperienceTag> attribute) {
        return JsonEnumHelper.toJson(attribute);
    }
    @Override public Set<ExperienceTag> convertToEntityAttribute(String dbData) {
        return JsonEnumHelper.fromJson(dbData, ExperienceTag.class);
    }
}
