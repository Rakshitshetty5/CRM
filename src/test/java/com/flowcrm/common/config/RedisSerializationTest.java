package com.flowcrm.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.flowcrm.common.enums.LeadStatus;
import com.flowcrm.dashboard.dto.DashboardSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RedisSerializationTest {

    @Test
    @SuppressWarnings("removal")
    void testDashboardSummaryResponseSerializationPreservesLongValuesInMap() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.configure(DeserializationFeature.USE_LONG_FOR_INTS, true);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        Map<LeadStatus, Long> leadsByStatus = new EnumMap<>(LeadStatus.class);
        leadsByStatus.put(LeadStatus.NEW, 4L);
        leadsByStatus.put(LeadStatus.QUALIFIED, 6L);

        DashboardSummaryResponse original = new DashboardSummaryResponse(
                10L,
                leadsByStatus,
                15L,
                8L,
                7L,
                2L
        );

        byte[] serialized = serializer.serialize(original);
        assertNotNull(serialized);

        Object deserializedObj = serializer.deserialize(serialized);
        assertNotNull(deserializedObj);
        assertInstanceOf(DashboardSummaryResponse.class, deserializedObj);

        DashboardSummaryResponse deserialized = (DashboardSummaryResponse) deserializedObj;
        assertEquals(10L, deserialized.totalLeads());
        assertEquals(15L, deserialized.totalTasks());

        Map<LeadStatus, Long> deserializedMap = deserialized.leadsByStatus();
        assertNotNull(deserializedMap);

        Object newValue = deserializedMap.get(LeadStatus.NEW);
        assertNotNull(newValue);
        assertInstanceOf(Long.class, newValue, "Value in leadsByStatus map should be Long, not Integer");
        assertEquals(4L, (Long) newValue);
    }
}
