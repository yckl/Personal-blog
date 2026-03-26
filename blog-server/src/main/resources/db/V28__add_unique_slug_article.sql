-- Add unique index for slug to prevent TooManyResultsException and routing collisions using idempotent stored procedure
DELIMITER //
CREATE PROCEDURE AddSlugIndexIfNotExists()
BEGIN
    DECLARE idx_count INT;
    SELECT COUNT(1) INTO idx_count FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() AND table_name = 'article' AND index_name = 'idx_article_slug';
    IF idx_count = 0 THEN
        ALTER TABLE article ADD UNIQUE INDEX idx_article_slug (slug);
    END IF;
END //
DELIMITER ;
CALL AddSlugIndexIfNotExists();
DROP PROCEDURE AddSlugIndexIfNotExists;
