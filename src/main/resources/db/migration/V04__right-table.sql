CREATE TABLE person
(
    id                BIGSERIAL PRIMARY KEY,
    norsk_ident       VARCHAR                  NOT NULL UNIQUE,
    created           TIMESTAMP WITH TIME ZONE NOT NULL default current_timestamp,
    last_synchronized TIMESTAMP WITH TIME ZONE NOT NULL default 'epoch'
);

CREATE TABLE rights
(
    id                  BIGSERIAL PRIMARY KEY,
    person_id           BIGINT references person (id) NOT NULL,
    organization_number VARCHAR                       NOT NULL,
    right_type          VARCHAR                       NOT NULL,
    valid_from          TIMESTAMP WITH TIME ZONE      NOT NULL,
    valid_to            TIMESTAMP WITH TIME ZONE,

    unique (person_id, organization_number, right_type, valid_from)

);

CREATE INDEX rights_person_id ON rights (person_id);
