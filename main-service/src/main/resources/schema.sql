/*DROP TABLE IF EXISTS participation_requests CASCADE;
DROP TABLE IF EXISTS compilation_events CASCADE;
DROP TABLE IF EXISTS compilations CASCADE;
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS users CASCADE;*/


CREATE TABLE IF NOT EXISTS users (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name VARCHAR(250) NOT NULL,
  email VARCHAR(254) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    annotation VARCHAR(2000) NOT NULL,
    description VARCHAR(7000) NOT NULL,

    category_id BIGINT NOT NULL,
    initiator_id BIGINT NOT NULL,

    event_date TIMESTAMP NOT NULL,
    created_on TIMESTAMP NOT NULL,
    published_on TIMESTAMP,

    lat REAL NOT NULL,
    lon REAL NOT NULL,

    paid BOOLEAN NOT NULL,
    participant_limit INT NOT NULL,
    request_moderation BOOLEAN NOT NULL,

    state VARCHAR(20) NOT NULL,

    CONSTRAINT fk_event_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_event_initiator FOREIGN KEY (initiator_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS compilations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS compilation_events (
    compilation_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,

    CONSTRAINT pk_compilation_events PRIMARY KEY (compilation_id, event_id),

    CONSTRAINT fk_compilation_events_compilation
        FOREIGN KEY (compilation_id)
        REFERENCES compilations(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_compilation_events_event
        FOREIGN KEY (event_id)
        REFERENCES events(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS participation_requests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created TIMESTAMP NOT NULL,

    CONSTRAINT uq_event_requester UNIQUE (event_id, requester_id),

    CONSTRAINT fk_request_event FOREIGN KEY (event_id)
        REFERENCES events(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_request_user FOREIGN KEY (requester_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);
