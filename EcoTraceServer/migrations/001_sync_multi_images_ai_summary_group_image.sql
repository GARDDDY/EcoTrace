-- Синхронизирует прод-БД (существовавшую ещё до появления папки migrations, т.е. на
-- уровне закоммиченного init.sql) с текущей схемой одной миграцией. Переносит:
--   1) мульти-фото постов/комментариев (post.image_key/comment.image_key -> post_image/comment_image,
--      с переносом уже существующих данных перед удалением старых колонок);
--   2) флаг "картинка группы сгенерирована" (group.image_generated);
--   3) таблицу ИИ-анализов калькулятора calc_ai_summary, сразу с разбивкой по периодам (7/30 дней).

ALTER TABLE "group" ADD COLUMN IF NOT EXISTS image_generated BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS post_image (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    post_id   BIGINT   NOT NULL REFERENCES post(id) ON DELETE CASCADE,
    image_key TEXT     NOT NULL,
    position  SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_post_image_post ON post_image (post_id, position);

CREATE TABLE IF NOT EXISTS comment_image (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comment_id BIGINT   NOT NULL REFERENCES comment(id) ON DELETE CASCADE,
    image_key  TEXT     NOT NULL,
    position   SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_comment_image_comment ON comment_image (comment_id, position);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'post' AND column_name = 'image_key') THEN
        INSERT INTO post_image (post_id, image_key, position)
        SELECT id, image_key, 0 FROM post WHERE image_key IS NOT NULL;
        ALTER TABLE post DROP COLUMN image_key;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'comment' AND column_name = 'image_key') THEN
        INSERT INTO comment_image (comment_id, image_key, position)
        SELECT id, image_key, 0 FROM comment WHERE image_key IS NOT NULL;
        ALTER TABLE comment DROP COLUMN image_key;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS calc_ai_summary (
    user_id       BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    calculator_id SMALLINT    NOT NULL REFERENCES calculator(id),
    period_days   SMALLINT    NOT NULL DEFAULT 7,
    summary       TEXT        NOT NULL,
    generated_for DATE        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, calculator_id, period_days)
);
