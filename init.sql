CREATE EXTENSION IF NOT EXISTS citext;

CREATE TYPE gender              AS ENUM ('unknown', 'male', 'female');
CREATE TYPE visibility          AS ENUM ('everyone', 'friends', 'nobody');
CREATE TYPE friendship_status   AS ENUM ('pending', 'accepted');
CREATE TYPE group_type          AS ENUM ('open', 'closed');
CREATE TYPE group_role          AS ENUM ('participant', 'assistant', 'manager', 'creator');
CREATE TYPE event_status        AS ENUM ('active', 'finished');
CREATE TYPE event_role          AS ENUM ('participant', 'assistant', 'creator');
CREATE TYPE event_start_cond    AS ENUM ('first_action', 'member_count');
CREATE TYPE filter_scope        AS ENUM ('user', 'event', 'group');
CREATE TYPE exp_reason          AS ENUM ('education', 'event', 'calculator', 'other');
CREATE TYPE auth_code_type      AS ENUM ('registration', 'password_reset');

CREATE TABLE app_user (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username        CITEXT      NOT NULL UNIQUE,
    email           CITEXT      NOT NULL UNIQUE,
    password_hash   TEXT        NOT NULL,
    fullname        TEXT,
    gender          gender      NOT NULL DEFAULT 'unknown',
    about_me        TEXT,
    country_code    CHAR(2),
    experience      INTEGER     NOT NULL DEFAULT 0,

    name_visibility     visibility NOT NULL DEFAULT 'everyone',
    country_visibility  visibility NOT NULL DEFAULT 'everyone',
    friends_visibility  visibility NOT NULL DEFAULT 'everyone',
    groups_visibility   visibility NOT NULL DEFAULT 'everyone',

    avatar_key      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_experience       ON app_user (experience DESC);
CREATE INDEX idx_user_country_exp      ON app_user (country_code, experience DESC);
CREATE INDEX idx_user_username_prefix  ON app_user (username text_pattern_ops);

CREATE TABLE experience_ledger (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    amount      INTEGER     NOT NULL,
    reason      exp_reason  NOT NULL,
    ref_id      BIGINT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_exp_ledger_user ON experience_ledger (user_id, created_at DESC);

CREATE TABLE friendship (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    requester_id BIGINT            NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    addressee_id BIGINT            NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    status       friendship_status NOT NULL DEFAULT 'pending',
    created_at   TIMESTAMPTZ       NOT NULL DEFAULT now(),
    responded_at TIMESTAMPTZ,
    CHECK (requester_id <> addressee_id)
);
CREATE UNIQUE INDEX uq_friend_pair
    ON friendship (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));
CREATE INDEX idx_friend_requester ON friendship (requester_id);
CREATE INDEX idx_friend_addressee ON friendship (addressee_id);

CREATE TABLE filter (
    id          SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scope       filter_scope NOT NULL,
    name        TEXT         NOT NULL,
    description TEXT,
    color_fg    CHAR(7),
    color_bg    CHAR(7),
    UNIQUE (scope, name)
);

CREATE TABLE user_filter (
    user_id   BIGINT   NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    filter_id SMALLINT NOT NULL REFERENCES filter(id)   ON DELETE CASCADE,
    PRIMARY KEY (user_id, filter_id)
);
CREATE INDEX idx_user_filter_filter ON user_filter (filter_id);

CREATE TABLE education_module (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title      TEXT    NOT NULL,
    image_key  TEXT,
    position   INTEGER NOT NULL DEFAULT 0,
    is_control BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE education_completion (
    user_id         BIGINT      NOT NULL REFERENCES app_user(id)         ON DELETE CASCADE,
    module_id       BIGINT      NOT NULL REFERENCES education_module(id) ON DELETE CASCADE,
    correct_answers INTEGER     NOT NULL DEFAULT 0,
    total_questions INTEGER     NOT NULL DEFAULT 0,
    completed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, module_id)
);

CREATE TABLE "group" (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            CITEXT      NOT NULL UNIQUE,
    creator_id      BIGINT      NOT NULL REFERENCES app_user(id),
    type            group_type  NOT NULL DEFAULT 'open',
    about           TEXT,
    rules           TEXT,
    rules_image_key TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE group_member (
    group_id  BIGINT     NOT NULL REFERENCES "group"(id) ON DELETE CASCADE,
    user_id   BIGINT     NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role      group_role NOT NULL DEFAULT 'participant',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id)
);
CREATE INDEX idx_group_member_user ON group_member (user_id);

CREATE TABLE group_filter (
    group_id  BIGINT   NOT NULL REFERENCES "group"(id) ON DELETE CASCADE,
    filter_id SMALLINT NOT NULL REFERENCES filter(id)  ON DELETE CASCADE,
    PRIMARY KEY (group_id, filter_id)
);
CREATE INDEX idx_group_filter_filter ON group_filter (filter_id);

CREATE TABLE post (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_id   BIGINT      NOT NULL REFERENCES "group"(id) ON DELETE CASCADE,
    author_id  BIGINT      NOT NULL REFERENCES app_user(id),
    text       TEXT,
    image_key  TEXT,
    pinned_at  TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_post_group_id ON post (group_id, id);

CREATE TABLE comment (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    post_id    BIGINT      NOT NULL REFERENCES post(id) ON DELETE CASCADE,
    author_id  BIGINT      NOT NULL REFERENCES app_user(id),
    text       TEXT,
    image_key  TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_comment_post ON comment (post_id, id);

CREATE TABLE event (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            TEXT             NOT NULL,
    about           TEXT,
    creator_id      BIGINT           NOT NULL REFERENCES app_user(id),
    status          event_status     NOT NULL DEFAULT 'active',
    start_condition event_start_cond NOT NULL DEFAULT 'first_action',
    start_threshold INTEGER,
    image_key       TEXT,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT now(),
    status_updated_at TIMESTAMPTZ
);
CREATE INDEX idx_event_status ON event (status);

CREATE TABLE event_member (
    event_id     BIGINT      NOT NULL REFERENCES event(id)    ON DELETE CASCADE,
    user_id      BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role         event_role  NOT NULL DEFAULT 'participant',
    validated_at TIMESTAMPTZ,
    joined_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, user_id)
);
CREATE INDEX idx_event_member_user ON event_member (user_id);

CREATE TABLE event_filter (
    event_id  BIGINT   NOT NULL REFERENCES event(id)  ON DELETE CASCADE,
    filter_id SMALLINT NOT NULL REFERENCES filter(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, filter_id)
);
CREATE INDEX idx_event_filter_filter ON event_filter (filter_id);

CREATE TABLE event_schedule_item (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id  BIGINT      NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    title     TEXT        NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at   TIMESTAMPTZ,
    position  INTEGER     NOT NULL DEFAULT 0
);
CREATE INDEX idx_schedule_event ON event_schedule_item (event_id, starts_at);

CREATE TABLE event_goal (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id BIGINT  NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    text     TEXT    NOT NULL,
    position INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_goal_event ON event_goal (event_id);

CREATE TABLE event_zone (
    id               BIGINT           NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    event_id         BIGINT           NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    schedule_item_id BIGINT           REFERENCES event_schedule_item(id) ON DELETE SET NULL,
    latitude         DOUBLE PRECISION NOT NULL,
    longitude        DOUBLE PRECISION NOT NULL,
    label            TEXT,
    is_zone          BOOLEAN          NOT NULL DEFAULT FALSE,
    radius_m         INTEGER,
    color_fill       CHAR(9),
    color_stroke     CHAR(9)
);
CREATE INDEX idx_zone_event ON event_zone (event_id);

CREATE TABLE calculator (
    id       SMALLINT PRIMARY KEY,
    name     TEXT    NOT NULL,
    position INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE calculator_question (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    calculator_id SMALLINT NOT NULL REFERENCES calculator(id) ON DELETE CASCADE,
    text          TEXT     NOT NULL,
    position      INTEGER  NOT NULL DEFAULT 0
);

CREATE TABLE calc_entry (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES app_user(id)  ON DELETE CASCADE,
    calculator_id SMALLINT    NOT NULL REFERENCES calculator(id),
    entry_date    DATE        NOT NULL,
    answers       JSONB       NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_calc_entry_user ON calc_entry (user_id, calculator_id, entry_date);

CREATE TABLE news_post (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source     TEXT,
    url        TEXT NOT NULL UNIQUE,
    image_url  TEXT,
    title      TEXT,
    is_russian BOOLEAN NOT NULL DEFAULT TRUE,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_news_fetched ON news_post (fetched_at DESC);

CREATE TABLE session (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token      TEXT        NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_session_user ON session (user_id);

CREATE TABLE auth_code (
    email      CITEXT         NOT NULL,
    type       auth_code_type NOT NULL,
    code       INTEGER        NOT NULL,
    expires_at TIMESTAMPTZ    NOT NULL,
    applied_at TIMESTAMPTZ,
    PRIMARY KEY (email, type)
);

INSERT INTO filter (scope, name, description, color_fg, color_bg) VALUES
    ('user', 'Активный',     '«Активный» пользователь всегда готов к новым начинаниям и активен в любом деле.', '#00FA9A', '#1A3329'),
    ('user', 'Веселый',      'Пользователь, который всегда в хорошем настроении и приносит позитив в любое общение.', '#FF7F50', '#33201A'),
    ('user', 'Открытый',     'Пользователь, который легко находит общий язык с окружающими.', '#00CED1', '#1A3333'),
    ('user', 'Онлайн',       'Пользователь, который часто бывает в сети.', '#DA70D6', '#331A32'),
    ('user', 'Трудолюбивый', 'Пользователь, который доводит начатое до конца.', '#FFD700', '#4B4B0E'),
    ('user', 'Экстраверт',   'Пользователь, любящий общение.', '#FF4500', '#4B1A1A'),
    ('event', 'На улице',       'Часть или всё мероприятие проходит на улице.', '#00FA9A', '#1A3329'),
    ('event', 'В помещении',    'Часть или всё мероприятие проходит в помещении.', '#FF7F50', '#33201A'),
    ('event', 'Образовательное','', '#00CED1', '#1A3333'),
    ('event', 'Для всей семьи', '', '#DA70D6', '#331A32'),
    ('group', 'Мероприятия',  'Группа, в которой делятся интересными мероприятиями.', '#00FA9A', '#1A3329'),
    ('group', 'По интересам', 'Группа, в которой пользователи общаются.', '#FF7F50', '#33201A'),
    ('group', 'Для новичков', '', '#00CED1', '#1A3333'),
    ('group', 'Творчество',   'Группа для любителей искусства и творчества.', '#DA70D6', '#331A32');

INSERT INTO calculator (id, name, position) VALUES
    (0, 'Пища', 0),
    (1, 'Вода', 1),
    (2, 'Отходы', 2),
    (3, 'Энергия', 3),
    (4, 'Транспорт', 4);

INSERT INTO calculator_question (calculator_id, text, position) VALUES
    (0, 'Виды мяса', 0),
    (0, 'Овощи и фрукты', 1),
    (0, 'Переработанные продукты', 2),
    (0, 'Местные и сезонные', 3);