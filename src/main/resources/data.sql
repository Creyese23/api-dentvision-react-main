-- =============================================
-- DENTVISION - Seed Data (solo roles)
-- El usuario admin y perfiles se crean via DataInitializer (JPA)
-- =============================================

-- Corregir columnas opcionales en perfiles_usuario que pueden ser NOT NULL
ALTER TABLE perfiles_usuario
    MODIFY COLUMN nombres VARCHAR(100) NULL DEFAULT '',
    MODIFY COLUMN apellidos VARCHAR(100) NULL DEFAULT '',
    MODIFY COLUMN tipo_identificacion VARCHAR(30) NULL DEFAULT '',
    MODIFY COLUMN identificacion VARCHAR(20) NULL DEFAULT '',
    MODIFY COLUMN telefono VARCHAR(20) NULL DEFAULT '',
    MODIFY COLUMN direccion VARCHAR(200) NULL DEFAULT '';

-- Roles
INSERT INTO roles (nombre_rol)
SELECT 'ROLE_ADMIN' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre_rol = 'ROLE_ADMIN');

INSERT INTO roles (nombre_rol)
SELECT 'ROLE_ODONTOLOGO' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre_rol = 'ROLE_ODONTOLOGO');

INSERT INTO roles (nombre_rol)
SELECT 'ROLE_RECEPCIONISTA' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre_rol = 'ROLE_RECEPCIONISTA');

INSERT INTO roles (nombre_rol)
SELECT 'ROLE_TECNICO' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre_rol = 'ROLE_TECNICO');

INSERT INTO roles (nombre_rol)
SELECT 'ROLE_PACIENTE' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre_rol = 'ROLE_PACIENTE');
