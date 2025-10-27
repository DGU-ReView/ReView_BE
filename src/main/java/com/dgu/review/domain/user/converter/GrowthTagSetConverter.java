package com.dgu.review.domain.user.converter;

import java.util.Set;

import com.dgu.review.domain.user.entity.GrowthTag;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class GrowthTagSetConverter implements AttributeConverter<Set<GrowthTag>, String> {
    @Override public String convertToDatabaseColumn(Set<GrowthTag> attribute) {
        return JsonEnumHelper.toJson(attribute);
    }
    @Override public Set<GrowthTag> convertToEntityAttribute(String dbData) {
        return JsonEnumHelper.fromJson(dbData, GrowthTag.class);
    }
}