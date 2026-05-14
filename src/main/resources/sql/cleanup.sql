-- Очищаем блокировку Liquibase
DELETE FROM databasechangeloglock;
INSERT INTO databasechangeloglock (ID, LOCKED) VALUES (1, false);

-- Удаляем все ENUM типы
DO $$
DECLARE
    enum_type_name text;
BEGIN
    FOR enum_type_name IN
        SELECT typname
        FROM pg_type
        WHERE typtype = 'e'
        AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')
    LOOP
        EXECUTE 'DROP TYPE IF EXISTS ' || enum_type_name || ' CASCADE';
    END LOOP;
END $$;