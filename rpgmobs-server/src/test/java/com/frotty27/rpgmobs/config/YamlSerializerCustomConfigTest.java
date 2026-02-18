package com.frotty27.rpgmobs.config;

import com.frotty27.rpgmobs.config.schema.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YamlSerializerCustomConfigTest {

    private static final class CustomConfig {
        @Cfg(file = "test.yml")
        @FixedArraySize(3)
        public int[] fixed = new int[]{1, 2, 3};

        @Cfg(file = "test.yml")
        @Min(0.0)
        @Max(1.0)
        public double clamped = 0.5;

        @Cfg(file = "test.yml")
        public Map<String, SubConfig> map = defaultMap();

        private static Map<String, SubConfig> defaultMap() {
            Map<String, SubConfig> m = new LinkedHashMap<>();
            SubConfig a = new SubConfig();
            a.value = 10;
            m.put("a", a);
            SubConfig b = new SubConfig();
            b.value = 20;
            m.put("b", b);
            return m;
        }
    }

    public static final class SubConfig {
        public int value = 0;
    }

    @Test
    void mapMergeAndClampingAndFixedArray(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("test.yml");
        String yaml = """
                fixed: [9, 9, 9, 9]
                clamped: 2.0
                map:
                  a:
                    value: 99
                  c:
                    value: 7
                """;
        Files.writeString(file, yaml, StandardCharsets.UTF_8);

        CustomConfig cfg = YamlSerializer.loadOrCreate(tempDir, new CustomConfig());

        assertNotNull(cfg.fixed);
        assertTrue(cfg.fixed.length == 3);
        for (int value : cfg.fixed) {
            assertTrue(value >= 0);
        }

        assertTrue(cfg.clamped >= 0.0 && cfg.clamped <= 1.0);

        assertNotNull(cfg.map);
        assertTrue(cfg.map.size() >= 2);
        assertTrue(cfg.map.containsKey("a"));
        assertTrue(cfg.map.containsKey("b"));
        assertTrue(cfg.map.containsKey("c"));
    }
}
