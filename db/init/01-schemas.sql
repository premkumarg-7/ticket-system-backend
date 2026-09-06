-- Runs once on Postgres first start (when data volume is empty).
-- Creates per-service schemas and grants on the single shared user.

CREATE SCHEMA IF NOT EXISTS auth   AUTHORIZATION ticket_user;
CREATE SCHEMA IF NOT EXISTS ticket AUTHORIZATION ticket_user;

GRANT ALL ON SCHEMA auth   TO ticket_user;
GRANT ALL ON SCHEMA ticket TO ticket_user;
