package com.flowcrm.notification.entity;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonMapConverterTest {

    private final JsonMapConverter converter = new JsonMapConverter();

    @Test
    void testConvertToDatabaseColumn() {
        Map<String, Object> map = Map.of("taskId", "task-123", "leadName", "Acme Corp");
        String json = converter.convertToDatabaseColumn(map);

        assertNotNull(json);
        assertTrue(json.contains("\"taskId\":\"task-123\""));
        assertTrue(json.contains("\"leadName\":\"Acme Corp\""));
    }

    @Test
    void testConvertToDatabaseColumnNullOrEmpty() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToDatabaseColumn(Map.of()));
    }

    @Test
    void testConvertToEntityAttribute() {
        String json = "{\"taskId\":\"task-123\",\"leadName\":\"Acme Corp\"}";
        Map<String, Object> map = converter.convertToEntityAttribute(json);

        assertNotNull(map);
        assertEquals("task-123", map.get("taskId"));
        assertEquals("Acme Corp", map.get("leadName"));
    }

    @Test
    void testConvertToEntityAttributeNullOrBlank() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute(""));
        assertNull(converter.convertToEntityAttribute("   "));
        assertNull(converter.convertToEntityAttribute("invalid json"));
    }
}
