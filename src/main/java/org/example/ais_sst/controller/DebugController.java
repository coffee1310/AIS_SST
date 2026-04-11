package org.example.ais_sst.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;  // ← Правильный импорт
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment env;  // ← Теперь правильный Environment

    @GetMapping("/config")
    public Map<String, String> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("db.url", env.getProperty("spring.datasource.url"));
        config.put("active.profiles", Arrays.toString(env.getActiveProfiles()));
        return config;
    }

    @GetMapping("/enum")
    public List<Map<String, Object>> checkEnum() {
        String query = """
            SELECT typname, enumlabel 
            FROM pg_enum 
            JOIN pg_type ON pg_enum.enumtypid = pg_type.oid 
            WHERE typname = 'genders'
        """;
        return jdbcTemplate.queryForList(query);
    }
}