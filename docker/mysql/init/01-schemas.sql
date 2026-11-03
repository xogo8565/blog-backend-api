-- 컨텍스트마다 스키마를 하나씩 만든다. 스키마를 넘는 FK는 만들지 않는다.
CREATE DATABASE IF NOT EXISTS blog_article CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS blog_comment CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS blog_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS blog_media CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 애플리케이션 계정(MYSQL_USER, 기본값 blog)에 blog_ 스키마 권한만 준다.
GRANT ALL PRIVILEGES ON `blog\_%`.* TO 'blog'@'%';
FLUSH PRIVILEGES;
