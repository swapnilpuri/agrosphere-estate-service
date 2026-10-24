CREATE TABLE estate (
 id uuid PRIMARY KEY, estate_code varchar(64) NOT NULL UNIQUE, name varchar(200) NOT NULL,
 country_code varchar(2) NOT NULL, timezone varchar(100) NOT NULL, total_area_hectares numeric(14,4),
 status varchar(8) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
 created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, version bigint NOT NULL DEFAULT 0,
 CHECK (total_area_hectares >= 0), CHECK (country_code ~ '^[A-Z]{2}$')
);
CREATE TABLE division (
 id uuid PRIMARY KEY, estate_id uuid NOT NULL REFERENCES estate(id), division_code varchar(64) NOT NULL,
 name varchar(200) NOT NULL, status varchar(8) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
 created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, version bigint NOT NULL DEFAULT 0,
 UNIQUE (estate_id,division_code), UNIQUE (id,estate_id)
);
CREATE TABLE rubber_clone (
 id uuid PRIMARY KEY, clone_code varchar(64) NOT NULL UNIQUE, description varchar(1000) NOT NULL,
 status varchar(8) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE'))
);
CREATE TABLE plantation_block (
 id uuid PRIMARY KEY, estate_id uuid NOT NULL REFERENCES estate(id), division_id uuid NOT NULL,
 block_code varchar(64) NOT NULL, name varchar(200) NOT NULL, crop_type varchar(20) NOT NULL CHECK (crop_type = 'RUBBER'),
 rubber_clone_code varchar(64) REFERENCES rubber_clone(clone_code), planting_date date,
 planted_area_hectares numeric(14,4) NOT NULL, planted_tree_count integer NOT NULL, tappable_tree_count integer NOT NULL,
 status varchar(8) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
 created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, version bigint NOT NULL DEFAULT 0,
 UNIQUE (estate_id,block_code), FOREIGN KEY (division_id,estate_id) REFERENCES division(id,estate_id),
 CHECK (planted_area_hectares >= 0 AND (status <> 'ACTIVE' OR planted_area_hectares > 0)),
 CHECK (planted_tree_count >= 0 AND tappable_tree_count >= 0 AND tappable_tree_count <= planted_tree_count)
);
CREATE INDEX estate_status_id ON estate(status,id);
CREATE INDEX division_estate_status_id ON division(estate_id,status,id);
CREATE INDEX block_estate_status_id ON plantation_block(estate_id,status,id);
CREATE INDEX block_division_id ON plantation_block(division_id);
CREATE INDEX block_clone_code ON plantation_block(rubber_clone_code);
CREATE INDEX clone_status_id ON rubber_clone(status,id);
ALTER TABLE outbox_event ADD COLUMN delivery_status varchar(10) NOT NULL DEFAULT 'PENDING' CHECK (delivery_status IN ('PENDING','PUBLISHED','FAILED'));
UPDATE outbox_event SET delivery_status='PUBLISHED' WHERE published_at IS NOT NULL;
ALTER TABLE outbox_event ADD COLUMN next_attempt_at timestamptz NOT NULL DEFAULT now();
CREATE INDEX outbox_dispatch_due ON outbox_event(next_attempt_at,occurred_at) WHERE delivery_status = 'PENDING';
