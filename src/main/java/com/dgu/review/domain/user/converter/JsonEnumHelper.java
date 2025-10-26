package com.dgu.review.domain.user.converter;

import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


final class JsonEnumHelper {
    static final ObjectMapper OM = new ObjectMapper();

    static <E extends Enum<E>> String toJson(Set<E> set) {
        try {
            if (set == null || set.isEmpty()) return "[]";
            return OM.writeValueAsString(set.stream().map(Enum::name).toList());
        } catch (Exception e) {
            throw new ApiException(ErrorCode.JSON_PARSE_FAIL);
           
        }
    }

    static <E extends Enum<E>> Set<E> fromJson(String json, Class<E> enumType) {
        try {
            if (json == null || json.isBlank()) return EnumSet.noneOf(enumType);
            List<String> names = OM.readValue(json, new TypeReference<List<String>>(){});
            if (names == null || names.isEmpty()) return EnumSet.noneOf(enumType);

            EnumSet<E> result = EnumSet.noneOf(enumType);
            for (String n : names) {
                result.add(Enum.valueOf(enumType, n));
            }
            return result;
        } catch (Exception e) {
            //빈집합 반환 
            return EnumSet.noneOf(enumType);
        }
    }
}




