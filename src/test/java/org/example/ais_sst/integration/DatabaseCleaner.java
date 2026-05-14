package org.example.ais_sst.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DatabaseCleaner {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void clearDatabase() {
        entityManager.flush();

        // Для PostgreSQL отключаем проверку внешних ключей по-другому
        entityManager.createNativeQuery("SET session_replication_role = 'replica'").executeUpdate();

        // Очищаем все таблицы (кроме системных Liquibase)
        List<String> tableNames = getTableNames();
        for (String tableName : tableNames) {
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName + " RESTART IDENTITY CASCADE").executeUpdate();
        }

        // Включаем проверку внешних ключей обратно
        entityManager.createNativeQuery("SET session_replication_role = 'origin'").executeUpdate();

        // Сбрасываем последовательности
        List<String> sequences = getSequences();
        for (String sequence : sequences) {
            entityManager.createNativeQuery("ALTER SEQUENCE " + sequence + " RESTART WITH 1").executeUpdate();
        }
    }

    private List<String> getTableNames() {
        return entityManager.createNativeQuery(
                "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                        "AND table_name NOT IN ('databasechangelog', 'databasechangeloglock')"
        ).getResultList();
    }

    private List<String> getSequences() {
        return entityManager.createNativeQuery(
                "SELECT sequence_name FROM information_schema.sequences " +
                        "WHERE sequence_schema = 'public'"
        ).getResultList();
    }
}