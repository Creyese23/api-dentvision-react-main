# DENTVISION API

API REST para el sistema de gestión de clínica odontológica **DENTVISION**, desarrollada con **Spring Boot 3.5.13** y **Java 21** en el marco del programa **Tecnólogo en Análisis y Desarrollo de Software (ADSO) — ficha 3067454** del SENA.

Instructor: **Jeyson David Zúñiga Gómez**

---

## Tecnologías

| Componente | Versión |
| --- | --- |
| Spring Boot | 3.5.13 |
| Java | 21 |
| Spring Security + JWT (jjwt) | 0.13.0 |
| Spring Data JPA / Hibernate | 6.x |
| Lombok | 1.18.34 |
| MySQL | 8+ |
| SQL Server | 2019+ |

---

## Módulos del sistema

| Módulo | Tablas principales |
| --- | --- |
| Identidad y acceso | `roles`, `usuarios`, `perfiles_usuario`, `refresh_tokens` |
| Pacientes | `pacientes` |
| Personal | `empleados` |
| Agenda clínica | `citas`, `citas_servicios`, `servicios`, `procedimientos` |
| Facturación y pagos | `facturas`, `detalle_factura`, `pagos` |
| Inventario | `insumos`, `movimientos_inventario` |
| Órdenes de laboratorio | `ordenes`, `orden_detalle`, `entregas` |
| Comunicación | `conversaciones`, `mensajes` |

---

## Estructura del proyecto

```
src/main/java/co/edu/sena/productsreact/
├── config/
│   ├── JpaAuditingConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   ├── CitaController.java
│   ├── ConversacionController.java
│   ├── EmpleadoController.java
│   ├── FacturaController.java
│   ├── InsumoController.java
│   ├── OrdenController.java
│   ├── PacienteController.java
│   ├── PagoController.java
│   ├── ProcedimientoController.java
│   └── ServicioController.java
├── dto/
│   ├── auth/          (AuthResponse, LoginRequest, RegisterRequest, UserDto)
│   ├── cita/          (CitaRequest, CitaResponse)
│   ├── comunicacion/  (MensajeRequest, MensajeResponse)
│   ├── empleado/      (EmpleadoRequest, EmpleadoResponse)
│   ├── factura/       (FacturaRequest, FacturaResponse, DetalleFacturaRequest)
│   ├── insumo/        (InsumoRequest, InsumoResponse, MovimientoRequest)
│   ├── orden/         (OrdenRequest, OrdenResponse)
│   ├── paciente/      (PacienteRequest, PacienteResponse)
│   ├── pago/          (PagoRequest, PagoResponse)
│   ├── procedimiento/ (ProcedimientoRequest, ProcedimientoResponse)
│   └── servicio/      (ServicioRequest, ServicioResponse)
├── entity/
│   ├── Cita.java             CitaServicio.java
│   ├── Conversacion.java     DetalleFactura.java
│   ├── Empleado.java         Entrega.java
│   ├── Factura.java          Insumo.java
│   ├── Mensaje.java          MovimientoInventario.java
│   ├── Orden.java            OrdenDetalle.java
│   ├── Paciente.java         Pago.java
│   ├── PerfilUsuario.java    Procedimiento.java
│   ├── RefreshToken.java     Rol.java
│   ├── Servicio.java         User.java
├── exception/
│   ├── ApiError.java
│   ├── DuplicateResourceException.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── repository/           (un repositorio por entidad)
├── security/
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java
│   └── JwtService.java
├── service/
│   ├── AuthService.java        CitaService.java
│   ├── ConversacionService.java EmpleadoService.java
│   ├── FacturaService.java     InsumoService.java
│   ├── OrdenService.java       PacienteService.java
│   ├── PagoService.java        ProcedimientoService.java
│   └── ServicioService.java
└── ProductsReactApiApplication.java
```

---

## Requisitos

- JDK 21
- Maven 3.9+
- MySQL 8+ **o** SQL Server 2019+

---

## Puesta en marcha

### 1. Copiar y editar el `.env`

```bash
cp .env.example .env
```

Variables obligatorias:

```env
# Base de datos (ajustar según perfil elegido)
DB_HOST=localhost
DB_PORT=3306
DB_NAME=dentvision
DB_USERNAME=root
DB_PASSWORD=tu_password

# JWT
JWT_SECRET=clave-base64-minimo-256bits
JWT_EXPIRATION_MS=86400000    # 1 día (access token)
JWT_REFRESH_DAYS=7            # refresh token

# Perfil activo
SPRING_PROFILES_ACTIVE=mysql-dev
```

Perfiles disponibles: `mysql-dev`, `mysql-prod`, `sqlserver-dev`, `sqlserver-prod`.

### 2. Arrancar

```bash
./mvnw spring-boot:run
```

La API queda en `http://localhost:8080`.

### 3. Datos semilla

Al iniciar por primera vez se insertan automáticamente:

| Dato | Valor |
| --- | --- |
| Email admin | `admin@dentvision.co` |
| Password | `password` |
| Rol | `ROLE_ADMIN` |
| Roles creados | `ROLE_ADMIN`, `ROLE_ODONTOLOGO`, `ROLE_RECEPCIONISTA`, `ROLE_TECNICO`, `ROLE_PACIENTE` |
| Servicios | Consulta General, Limpieza Dental, Extracción Simple, Blanqueamiento, Ortodoncia, Endodoncia |
| Insumos | Guantes, Tapabocas, Anestesia, Resina |

---

## Endpoints

### Autenticación — `POST /auth/**` (público)

| Método | Ruta | Body | Respuesta |
| --- | --- | --- | --- |
| POST | `/auth/register` | `{ email, password, nombres?, apellidos?, telefono? }` | `201 { accessToken, refreshToken, user }` |
| POST | `/auth/login` | `{ email, password }` | `200 { accessToken, refreshToken, user }` |
| POST | `/auth/refresh` | `{ refreshToken }` | `200 { accessToken, refreshToken, user }` |

Registrar un nuevo usuario crea automáticamente el perfil y asigna `ROLE_PACIENTE`.

---

### Pacientes — `/api/pacientes`

| Método | Ruta | Roles permitidos |
| --- | --- | --- |
| GET | `/api/pacientes` | ADMIN, ODONTOLOGO, RECEPCIONISTA |
| GET | `/api/pacientes/buscar?q=texto` | ADMIN, ODONTOLOGO, RECEPCIONISTA |
| GET | `/api/pacientes/{id}` | ADMIN, ODONTOLOGO, RECEPCIONISTA, PACIENTE |
| POST | `/api/pacientes` | ADMIN, RECEPCIONISTA |
| PUT | `/api/pacientes/{id}` | ADMIN, RECEPCIONISTA |
| DELETE | `/api/pacientes/{id}` | ADMIN (soft-delete) |

---

### Empleados — `/api/empleados`

| Método | Ruta | Roles permitidos |
| --- | --- | --- |
| GET | `/api/empleados` | ADMIN, RECEPCIONISTA |
| GET | `/api/empleados/cargo/{cargo}` | ADMIN, RECEPCIONISTA |
| GET | `/api/empleados/{id}` | ADMIN, ODONTOLOGO, RECEPCIONISTA |
| POST | `/api/empleados` | ADMIN |
| PUT | `/api/empleados/{id}` | ADMIN |
| DELETE | `/api/empleados/{id}` | ADMIN (soft-delete) |

---

### Servicios — `/api/servicios`

| Método | Ruta | Roles permitidos |
| --- | --- | --- |
| GET | `/api/servicios` | Público |
| GET | `/api/servicios/{id}` | Público |
| POST | `/api/servicios` | ADMIN |
| PUT | `/api/servicios/{id}` | ADMIN |
| DELETE | `/api/servicios/{id}` | ADMIN (desactiva) |

---

### Citas — `/api/citas`

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/citas/paciente/{id}` | Citas de un paciente |
| GET | `/api/citas/odontologo/{id}` | Agenda de un odontólogo |
| GET | `/api/citas/rango?inicio=&fin=` | Citas en rango de fechas (ISO 8601) |
| GET | `/api/citas/{id}` | Detalle de cita |
| POST | `/api/citas` | Crear cita |
| PUT | `/api/citas/{id}` | Actualizar cita |
| PATCH | `/api/citas/{id}/estado?estado=` | Cambiar estado (`PROGRAMADA`, `ATENDIDA`, `CANCELADA`) |
| DELETE | `/api/citas/{id}` | Cancelar cita (soft-delete) |

---

### Procedimientos — `/api/procedimientos`

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/procedimientos/cita/{citaId}` | Procedimientos de una cita |
| GET | `/api/procedimientos/{id}` | Detalle |
| POST | `/api/procedimientos` | Crear procedimiento |
| PUT | `/api/procedimientos/{id}` | Actualizar |
| PATCH | `/api/procedimientos/{id}/estado?estado=` | Cambiar estado (`EN_PROCESO`, `COMPLETADO`) |
| DELETE | `/api/procedimientos/{id}` | Eliminar (soft-delete, solo ADMIN) |

---

### Facturación — `/api/facturas` y `/api/pagos`

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/facturas/paciente/{id}` | Facturas de un paciente |
| GET | `/api/facturas/{id}` | Detalle de factura |
| POST | `/api/facturas` | Crear factura (estado `PENDIENTE`, total `0`) |
| POST | `/api/facturas/{id}/detalle` | Agregar servicio a factura (recalcula total) |
| PATCH | `/api/facturas/{id}/estado?estado=` | Cambiar estado |
| DELETE | `/api/facturas/{id}` | Eliminar (soft-delete, solo ADMIN) |
| GET | `/api/pagos/factura/{facturaId}` | Pagos de una factura |
| POST | `/api/pagos` | Registrar pago (marca factura como `PAGADA` si cubre el total) |
| DELETE | `/api/pagos/{id}` | Anular pago (solo ADMIN) |

---

### Inventario — `/api/insumos`

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/insumos` | Listar insumos activos |
| GET | `/api/insumos/bajo-stock` | Insumos con `stockActual <= stockMinimo` |
| GET | `/api/insumos/{id}` | Detalle |
| POST | `/api/insumos` | Crear insumo (solo ADMIN) |
| PUT | `/api/insumos/{id}` | Actualizar (solo ADMIN) |
| POST | `/api/insumos/movimiento` | Entrada o salida de stock (valida stock disponible) |
| DELETE | `/api/insumos/{id}` | Desactivar (solo ADMIN) |

Body de movimiento:
```json
{ "idInsumo": 1, "idEmpleado": 2, "cantidad": 10, "tipoMovimiento": "ENTRADA" }
```
`tipoMovimiento`: `ENTRADA` o `SALIDA`.

---

### Órdenes de laboratorio — `/api/ordenes`

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/ordenes?estado=PENDIENTE` | Listar por estado |
| GET | `/api/ordenes/procedimiento/{id}` | Órdenes de un procedimiento |
| GET | `/api/ordenes/{id}` | Detalle |
| POST | `/api/ordenes` | Crear orden (crea la entrega automáticamente) |
| PATCH | `/api/ordenes/{id}/estado?estado=` | Cambiar estado (`PENDIENTE`, `EN_PROCESO`, `ENTREGADA`) |

---

### Comunicación — `/api/conversaciones`

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/api/conversaciones/iniciar?pacienteId=&procedimientoId=` | Iniciar conversación |
| GET | `/api/conversaciones/{id}/mensajes` | Mensajes de una conversación |
| POST | `/api/conversaciones/mensajes` | Enviar mensaje |
| PATCH | `/api/conversaciones/{id}/cerrar` | Cerrar conversación |

---

## Seguridad

- `/auth/**` y `GET /api/servicios` son públicos. El resto requiere JWT válido en `Authorization: Bearer <token>`.
- Los permisos por endpoint se controlan con `@PreAuthorize` a nivel de método.
- Los tokens de acceso se firman con **HS256**; la clave viene de `JWT_SECRET`.
- Las contraseñas se almacenan con **BCrypt** (strength 10).
- Al hacer login se revocan todos los refresh tokens anteriores del usuario.

---

## Formato de errores

Todos los errores responden con la misma estructura:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Errores de validación en los datos enviados",
  "path": "/api/pacientes",
  "timestamp": "2026-06-12T10:15:30",
  "fields": {
    "documento": "no debe estar vacío"
  }
}
```

| Caso | Status HTTP |
| --- | --- |
| Validación de campos | 400 |
| JSON mal formado | 400 |
| Credenciales inválidas / token expirado | 401 |
| Sin permiso para el recurso | 403 |
| Recurso no encontrado | 404 |
| Email / documento duplicado | 409 |
| Error inesperado | 500 |

---

## Ejemplos con curl

**Registro:**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@email.com","password":"Clave1234!","nombres":"Maria","apellidos":"Lopez"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@dentvision.co","password":"password"}'
```

**Crear paciente (requiere token):**
```bash
curl -X POST http://localhost:8080/api/pacientes \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nombres":"Carlos","apellidos":"Ruiz","documento":"12345678","tipoIdentificacion":"CC","telefono":"3001234567","genero":"M","fechaNacimiento":"1990-05-15"}'
```

**Registrar movimiento de inventario:**
```bash
curl -X POST http://localhost:8080/api/insumos/movimiento \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"idInsumo":1,"cantidad":50,"tipoMovimiento":"ENTRADA"}'
```

---

## Notas

- El archivo `.env` **no debe commitearse**. Ya está en `.gitignore`.
- En los perfiles `-prod` se recomienda cambiar `ddl-auto` a `validate` para no modificar el esquema en producción.
- Si cambias `JWT_SECRET` en caliente, todos los tokens activos dejan de ser válidos.
- Los perfiles de base de datos (`mysql-dev`, etc.) se configuran en los archivos `application-{perfil}.properties`.
