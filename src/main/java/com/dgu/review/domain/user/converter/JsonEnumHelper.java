//package com.dgu.review.domain.user.converter;
//
//import com.dgu.review.domain.user.entity.ExperienceTag;
//import com.dgu.review.domain.user.entity.GrowthTag;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.persistence.AttributeConverter;
//import jakarta.persistence.Converter;
//
//import java.util.Collections;
//import java.util.EnumSet;
//import java.util.List;
//import java.util.Set;
//
//
//final class JsonEnumHelper {
//    static final ObjectMapper OM = new ObjectMapper();
//
//    static <E extends Enum<E>> String toJson(Set<E> set) {
//        try {
//            if (set == null || set.isEmpty()) return "[]";
//            // enum name() 목록으로 저장 (안정적인 키)
//            return OM.writeValueAsString(set.stream().map(Enum::name).toList());
//        } catch (Exception e) {
//            throw new IllegalStateException("Enum set -> JSON 직렬화 실패", e);
//        }
//    }
//
//    static <E extends Enum<E>> Set<E> fromJson(String json, Class<E> enumType) {
//        try {
//            if (json == null || json.isBlank()) return EnumSet.noneOf(enumType);
//            List<String> names = OM.readValue(json, new TypeReference<List<String>>(){});
//            if (names == null || names.isEmpty()) return EnumSet.noneOf(enumType);
//
//            EnumSet<E> result = EnumSet.noneOf(enumType);
//            for (String n : names) {
//                result.add(Enum.valueOf(enumType, n));
//            }
//            return result;
//        } catch (Exception e) {
//            // 깨진 데이터 방어: 빈 집합으로 복구
//            return EnumSet.noneOf(enumType);
//        }
//    }
//}
//
//@Converter(autoApply = false)
//public class ExperienceTagSetConverter implements AttributeConverter<Set<ExperienceTag>, String> {
//    @Override public String convertToDatabaseColumn(Set<ExperienceTag> attribute) {
//        return JsonEnumHelper.toJson(attribute);
//    }
//    @Override public Set<ExperienceTag> convertToEntityAttribute(String dbData) {
//        return JsonEnumHelper.fromJson(dbData, ExperienceTag.class);
//    }
//}
//
//@Converter(autoApply = false)
//public class GrowthTagSetConverter implements AttributeConverter<Set<GrowthTag>, String> {
//    @Override public String convertToDatabaseColumn(Set<GrowthTag> attribute) {
//        return JsonEnumHelper.toJson(attribute);
//    }
//    @Override public Set<GrowthTag> convertToEntityAttribute(String dbData) {
//        return JsonEnumHelper.fromJson(dbData, GrowthTag.class);
//    }
//}
