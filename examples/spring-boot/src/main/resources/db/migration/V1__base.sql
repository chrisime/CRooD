CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS "abcd"
(
    id           bigserial,
    identifier   uuid        DEFAULT uuid_generate_v4(),
    nickname     varchar(31) NOT NULL,
    amount       smallint    NOT NULL,
    is_available boolean     NOT NULL,

    created_ts   timestamp DEFAULT timezone('UTC', now()),
    modified_ts  timestamp DEFAULT timezone('UTC', now()),

    version      bigint,

    CONSTRAINT pk_abcd PRIMARY KEY (id),
    CONSTRAINT uq_abcd_name UNIQUE (nickname)
);

CREATE TABLE IF NOT EXISTS "efgh"
(
    id          uuid        NOT NULL DEFAULT uuid_generate_v4(),
    identifier  uuid        DEFAULT uuid_generate_v4(),
    email       varchar(63) NOT NULL,

    created_ts  timestamp   DEFAULT timezone('UTC', now()),
    modified_ts timestamp   DEFAULT timezone('UTC', now()),

    abcd_id     bigint      NOT NULL REFERENCES abcd(id),

    version     bigint,

    CONSTRAINT pk_efgh PRIMARY KEY (id),
    CONSTRAINT uq_efgh_email UNIQUE (email),
    CONSTRAINT fk_efgh_abcd FOREIGN KEY (abcd_id) REFERENCES abcd(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_abcd_efgh ON efgh(abcd_id);

CREATE TABLE IF NOT EXISTS "ijkl"
(
    abcd_id bigint NOT NULL,
    efgh_id uuid   NOT NULL,

    CONSTRAINT fk_abcd FOREIGN KEY (abcd_id) REFERENCES abcd(id),
    CONSTRAINT fk_efgh FOREIGN KEY (efgh_id) REFERENCES efgh(id)
);

CREATE OR REPLACE FUNCTION modified_tstamp_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.modified_ts = timezone('UTC'::text, now());
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE OR REPLACE FUNCTION created_tstamp_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.created_ts = timezone('UTC'::text, now());
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_abcd_modified_ts_column
    BEFORE UPDATE
    ON "abcd"
    FOR EACH ROW
EXECUTE PROCEDURE modified_tstamp_column();

CREATE TRIGGER update_efgh_modified_ts_column
    BEFORE UPDATE
    ON "efgh"
    FOR EACH ROW
EXECUTE PROCEDURE modified_tstamp_column();

CREATE TRIGGER insert_abcd_creation_ts_column
    BEFORE INSERT
    ON "abcd"
    FOR EACH ROW
EXECUTE PROCEDURE created_tstamp_column();

CREATE TRIGGER insert_efgh_creation_ts_column
    BEFORE INSERT
    ON "efgh"
    FOR EACH ROW
EXECUTE PROCEDURE created_tstamp_column();
