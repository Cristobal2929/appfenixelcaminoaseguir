-- Tabla del Jardín: identidad ligera (nombre + PIN de 4 dígitos).
-- Ejecutar UNA sola vez en el SQL Editor de Supabase, en el mismo
-- proyecto donde ya viven fenix_conversaciones, fenix_lecciones, etc.

create table if not exists fenix_identidades (
    id bigint generated always as identity primary key,
    nombre_normalizado text not null unique,
    nombre_visible text not null,
    pin_hash text not null,
    created_at timestamptz not null default now()
);

-- Búsqueda por nombre normalizado ya es rápida por el unique de arriba,
-- pero se deja el índice explícito por claridad y por si el unique
-- cambiara en el futuro.
create index if not exists idx_fenix_identidades_nombre
    on fenix_identidades (nombre_normalizado);
