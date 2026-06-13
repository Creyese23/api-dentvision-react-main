-- =============================================
-- DENTVISION - Seed Data (MySQL compatible)
-- =============================================

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

-- Usuario admin (password: Admin1234!)
-- Se obtiene id_rol directamente con FROM roles para compatibilidad MySQL
INSERT INTO usuarios (email, password, estado, id_rol)
SELECT 'admin@dentvision.co',
       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
       true,
       r.id
FROM roles r
WHERE r.nombre_rol = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'admin@dentvision.co');

INSERT INTO perfiles_usuario (id_usuario, nombres, apellidos, telefono)
SELECT u.id, 'Administrador', 'DENTVISION', '3001234567'
FROM usuarios u
WHERE u.email = 'admin@dentvision.co'
  AND NOT EXISTS (SELECT 1 FROM perfiles_usuario p WHERE p.id_usuario = u.id);

-- Servicios dentales
INSERT INTO servicios (nombre, descripcion, precio, duracion_minutos, estado)
SELECT 'Consulta General', 'Revisión y diagnóstico dental general', 50000.00, 30, true
WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE nombre = 'Consulta General');

INSERT INTO servicios (nombre, descripcion, precio, duracion_minutos, estado)
SELECT 'Limpieza Dental', 'Profilaxis y detartraje ultrasónico', 80000.00, 45, true
WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE nombre = 'Limpieza Dental');

INSERT INTO servicios (nombre, descripcion, precio, duracion_minutos, estado)
SELECT 'Extracción Simple', 'Extracción de pieza dental sin complicaciones', 120000.00, 30, true
WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE nombre = 'Extracción Simple');

INSERT INTO servicios (nombre, descripcion, precio, duracion_minutos, estado)
SELECT 'Blanqueamiento Dental', 'Blanqueamiento con luz LED profesional', 350000.00, 60, true
WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE nombre = 'Blanqueamiento Dental');

INSERT INTO servicios (nombre, descripcion, precio, duracion_minutos, estado)
SELECT 'Ortodoncia - Consulta', 'Valoración y plan de tratamiento ortodóntico', 80000.00, 45, true
WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE nombre = 'Ortodoncia - Consulta');

INSERT INTO servicios (nombre, descripcion, precio, duracion_minutos, estado)
SELECT 'Endodoncia', 'Tratamiento de conducto radicular', 450000.00, 90, true
WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE nombre = 'Endodoncia');

-- Insumos básicos
INSERT INTO insumos (nombre, descripcion, unidad_medida, stock_actual, stock_minimo, precio_unitario, estado)
SELECT 'Guantes de Nitrilo Talla M', 'Guantes desechables para procedimientos', 'Caja x100', 20, 5, 25000.00, true
WHERE NOT EXISTS (SELECT 1 FROM insumos WHERE nombre = 'Guantes de Nitrilo Talla M');

INSERT INTO insumos (nombre, descripcion, unidad_medida, stock_actual, stock_minimo, precio_unitario, estado)
SELECT 'Tapabocas Quirúrgico', 'Mascarilla quirúrgica triple capa', 'Caja x50', 30, 10, 15000.00, true
WHERE NOT EXISTS (SELECT 1 FROM insumos WHERE nombre = 'Tapabocas Quirúrgico');

INSERT INTO insumos (nombre, descripcion, unidad_medida, stock_actual, stock_minimo, precio_unitario, estado)
SELECT 'Anestesia Lidocaína 2%', 'Anestesia local tubos de 1.8ml', 'Caja x50', 10, 5, 85000.00, true
WHERE NOT EXISTS (SELECT 1 FROM insumos WHERE nombre = 'Anestesia Lidocaína 2%');

INSERT INTO insumos (nombre, descripcion, unidad_medida, stock_actual, stock_minimo, precio_unitario, estado)
SELECT 'Resina Compuesta A2', 'Resina composite para restauraciones', 'Jeringa 4g', 8, 3, 45000.00, true
WHERE NOT EXISTS (SELECT 1 FROM insumos WHERE nombre = 'Resina Compuesta A2');
