-- Tabla del Templo de Gratitud. Ejecutar UNA sola vez en el SQL Editor
-- de Supabase, en el mismo proyecto donde ya vive fenix_identidades.

create table if not exists fenix_gratitudes (
    id bigint generated always as identity primary key,
    nombre_normalizado text not null,
    nombre_visible text not null,
    texto text not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_fenix_gratitudes_nombre
    on fenix_gratitudes (nombre_normalizado);

-- IMPORTANTE: igual que con fenix_identidades, esta tabla nace con Row
-- Level Security activado por defecto en Supabase, lo que bloquea
-- cualquier escritura hasta que se desactive (o se le agreguen políticas).
-- Se desactiva aquí mismo para no repetir el mismo error 42501 de antes.
alter table fenix_gratitudes disable row level security;
