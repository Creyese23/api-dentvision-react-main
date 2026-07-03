# DentVision API

API REST para el sistema de gestión de clínica odontológica **DENTVISION**, desarrollada con **Spring Boot 3.x** y **Java 21** en el marco del programa **Tecnólogo en Análisis y Desarrollo de Software (ADSO) — ficha 3067454** del SENA.

Instructor: **Jeyson David Zúñiga Gómez**

---

## Tecnologías

| Herramienta | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.x | Framework principal |
| Spring Security | 6 | Autenticación y autorización |
| Spring Data JPA | — | Acceso a base de datos |
| JWT (jjwt) | — | Tokens de autenticación sin estado |
| OAuth2 | — | Inicio de sesión con Google / GitHub |
| Lombok | — | Reducción de boilerplate (getters, setters, constructores) |
| MercadoPago SDK | — | Pasarela de pago Colombia |
| PayPal SDK | — | Pasarela de pago internacional |
| Brevo / SMTP | — | Envío de correos electrónicos |
| MySQL / PostgreSQL | — | Base de datos relacional |
| Maven | — | Gestión de dependencias y build |

---

## Arquitectura

```
Request HTTP
    ↓
[Filter: JwtAuthenticationFilter]   ← valida Bearer token, carga UserDetails
    ↓
[Controller]                        ← recibe request, delega al service
    ↓
[Service]                           ← lógica de negocio, transacciones
    ↓
[Repository]                        ← acceso a BD vía JPA / JPQL
    ↓
[Entity / Database]
```

### Paquetes principales

```
co.edu.sena.productsreact/
├── config/            # Configuración de Spring Security, JPA Auditing, Beans, Seeders
├── security/          # JWT Service, Filtro JWT, UserDetailsService, OAuth2 Handler
├── entity/            # Entidades JPA (User, Paciente, Cita, Factura, Orden, etc.)
├── repository/        # Interfaces JPA con queries personalizadas
├── service/           # Lógica de negocio por dominio
├── controller/        # Endpoints REST anotados con @RestController
├── dto/               # Objetos de transferencia de datos (Request / Response)
│   ├── auth/
│   ├── cita/
│   ├── factura/
│   ├── insumo/
│   ├── orden/
│   ├── paciente/
│   ├── pago/
│   ├── perfil/
│   ├── procedimiento/
│   ├── servicio/
│   └── usuario/
└── exception/         # Manejador global de excepciones, DTOs de error
```

---

## Entidades del dominio

| Entidad | Descripción |
|---|---|
| `User` | Usuario del sistema con email, contraseña cifrada con BCrypt y rol |
| `Rol` | Rol de seguridad: ROLE_ADMIN, ROLE_ODONTOLOGO, ROLE_RECEPCIONISTA, ROLE_TECNICO, ROLE_PACIENTE |
| `PerfilUsuario` | Datos personales extendidos del usuario (nombres, teléfono, documento) |
| `Paciente` | Paciente vinculado a un User; incluye historial de alergias |
| `Empleado` | Empleado vinculado a un User; incluye cargo y especialidad |
| `Cita` | Cita odontológica entre paciente y odontólogo con fecha, hora y estado |
| `CitaServicio` | Relación M-M entre Cita y Servicio |
| `Servicio` | Servicio odontológico con nombre, descripción, valor y duración |
| `SubServicio` | Subdivisión de un servicio (ej. Limpieza → Tartrectomía) |
| `Procedimiento` | Procedimiento ejecutado en una cita; tiene estado y notas médicas |
| `Orden` | Orden de laboratorio derivada de un procedimiento |
| `Entrega` | Registro de entrega de una orden de laboratorio |
| `Insumo` | Insumo de inventario con stock y punto de reorden |
| `MovimientoInventario` | Entrada/salida de insumos con responsable y fecha |
| `Factura` | Factura asociada a un paciente con detalles de cobro |
| `DetalleFactura` | Ítem individual de una factura (concepto, cantidad, valor) |
| `Pago` | Registro de pago de factura con método y referencia de transacción |
| `Conversacion` | Hilo de chat entre paciente y técnico/odontólogo |
| `Mensaje` | Mensaje individual dentro de una conversación |
| `RefreshToken` | Token de refresco de sesión con expiración |
| `Notificacion` | Notificación in-app con tipo, mensaje, leida y creadaEn — vinculada al usuario destinatario |

---

## Endpoints principales

### Autenticación — `/auth`

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/login` | Inicio de sesión; retorna accessToken + refreshToken |
| POST | `/auth/register` | Registro de nuevo paciente |
| POST | `/auth/refresh` | Renovar accessToken usando refreshToken |
| POST | `/auth/forgot-password` | Enviar email de recuperación de contraseña |
| POST | `/auth/reset-password` | Resetear contraseña con token del email |
| GET | `/auth/verify-email` | Verificar cuenta con token de confirmación |
| POST | `/auth/logout` | Cerrar sesión e invalidar refreshToken |

### Usuarios — `/api/usuarios` (ADMIN)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/usuarios` | Listar todos los usuarios |
| POST | `/api/usuarios` | Crear usuario |
| PATCH | `/api/usuarios/{id}/rol` | Cambiar rol de usuario |
| PATCH | `/api/usuarios/{id}/estado` | Activar / desactivar usuario |

### Pacientes — `/api/pacientes`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/pacientes` | Listar todos (ADMIN, ODONTOLOGO, RECEPCIONISTA) |
| GET | `/api/pacientes/me` | Mis datos como paciente |
| POST | `/api/pacientes` | Crear paciente (ADMIN) |
| PUT | `/api/pacientes/{id}` | Actualizar paciente |
| DELETE | `/api/pacientes/{id}` | Eliminar paciente (ADMIN) |
| GET | `/api/pacientes/pendientes` | Pacientes pendientes de activación (ADMIN) |
| PATCH | `/api/pacientes/{id}/activar` | Activar paciente |

### Citas — `/api/citas`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/citas` | Listar citas con filtros (rango de fechas, estado) |
| GET | `/api/citas/mis-citas` | Citas del paciente autenticado |
| GET | `/api/citas/odontologo/{id}` | Citas de un odontólogo |
| GET | `/api/citas/disponibles` | Horarios disponibles de un odontólogo |
| POST | `/api/citas` | Agendar cita (ADMIN / RECEPCIONISTA) |
| POST | `/api/citas/agendar` | Agendar cita (PACIENTE) |
| PATCH | `/api/citas/{id}/estado` | Cambiar estado de cita |
| DELETE | `/api/citas/{id}` | Cancelar cita |

### Procedimientos — `/api/procedimientos`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/procedimientos` | Listar procedimientos |
| GET | `/api/procedimientos/cita/{citaId}` | Procedimientos de una cita |
| POST | `/api/procedimientos` | Crear procedimiento |
| PATCH | `/api/procedimientos/{id}/estado` | Actualizar estado |
| PUT | `/api/procedimientos/{id}` | Actualizar procedimiento |

### Órdenes — `/api/ordenes`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/ordenes` | Listar órdenes por estado o asignadas |
| GET | `/api/ordenes/{id}` | Detalle de una orden |
| POST | `/api/ordenes` | Crear orden de laboratorio |
| PATCH | `/api/ordenes/{id}/estado` | Cambiar estado |
| POST | `/api/ordenes/{id}/imagen` | Subir imagen de resultado |

### Facturas — `/api/facturas`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/facturas/paciente/{id}` | Facturas de un paciente |
| POST | `/api/facturas` | Crear factura |
| POST | `/api/facturas/{id}/detalle` | Agregar ítem a factura |
| PATCH | `/api/facturas/{id}/estado` | Cambiar estado de factura |
| POST | `/api/facturas/desde-cita/{citaId}` | Generar factura desde cita |
| DELETE | `/api/facturas/{id}` | Eliminar factura |

### Pagos — `/api/pagos`

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/pagos` | Registrar pago manual (Efectivo, Otro) |
| GET | `/api/pagos/factura/{id}` | Pagos de una factura |
| DELETE | `/api/pagos/{id}` | Eliminar pago |
| POST | `/api/pagos/mercadopago/preferencia` | Crear preferencia MercadoPago |
| POST | `/api/pagos/mercadopago/webhook` | Webhook de notificaciones MP |
| POST | `/api/pagos/paypal/crear-orden` | Crear orden PayPal |
| POST | `/api/pagos/paypal/capturar` | Capturar pago PayPal |

### Insumos — `/api/insumos`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/insumos` | Listar insumos activos |
| GET | `/api/insumos/bajo-stock` | Insumos bajo el punto de reorden |
| POST | `/api/insumos` | Crear insumo |
| PUT | `/api/insumos/{id}` | Actualizar insumo |
| POST | `/api/insumos/{id}/movimiento` | Registrar entrada o salida de stock |

### Entregas — `/api/entregas`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/entregas` | Listar entregas |
| GET | `/api/entregas/mis-entregas` | Entregas propias (TECNICO) |
| PATCH | `/api/entregas/{id}/estado` | Actualizar estado de entrega |

### Conversaciones — `/api/conversaciones`

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/conversaciones` | Iniciar conversación |
| GET | `/api/conversaciones/mis-conversaciones` | Conversaciones del paciente |
| GET | `/api/conversaciones/{id}/mensajes` | Mensajes de una conversación |
| POST | `/api/conversaciones/{id}/mensajes` | Enviar mensaje |
| PATCH | `/api/conversaciones/{id}/cerrar` | Cerrar conversación |

### Notificaciones — `/api/notificaciones`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/notificaciones/mis` | Listar todas las notificaciones del usuario autenticado |
| GET | `/api/notificaciones/mis/no-leidas` | Listar notificaciones no leídas |
| PATCH | `/api/notificaciones/marcar-todas-leidas` | Marcar todas como leídas |

---

## Seguridad

- **JWT Access Token:** 15 minutos de vigencia, firmado con clave secreta HS256.
- **Refresh Token:** 7 días, almacenado en BD para invalidación explícita.
- **@PreAuthorize:** anotaciones por método para control granular de permisos.
- **CORS:** configurado para permitir el origen del frontend en desarrollo y producción.
- **Contraseñas:** cifradas con **BCrypt** (algoritmo de hashing adaptativo con sal aleatoria incluida; factor de costo 12). No se utiliza SHA-256 ni ningún hash de propósito general para contraseñas.
- **OAuth2:** autenticación con Google o GitHub genera JWT propio del sistema.
- **Notificación de login:** al iniciar sesión (credenciales o OAuth2), se guarda automáticamente una notificación de tipo `LOGIN` en la tabla `notificaciones`. Esta notificación aparece en la campana de la interfaz del usuario. **No se envía ningún correo electrónico de alerta de seguridad.**

---

## Variables de entorno

Configura las siguientes variables en Render (o en tu `.env` local):

```env
# Base de datos
DB_URL=jdbc:mysql://host:3306/dentvision
DB_USERNAME=usuario
DB_PASSWORD=contraseña

# JWT
JWT_SECRET=clave-secreta-muy-larga-y-segura
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# OAuth2
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
FRONTEND_URL=https://tu-frontend.onrender.com

# Email (Brevo/SMTP)
BREVO_API_KEY=...
MAIL_FROM=no-reply@dentvision.com

# MercadoPago
MP_ACCESS_TOKEN=APP_USR-...

# PayPal
PAYPAL_CLIENT_ID=...
PAYPAL_CLIENT_SECRET=...
PAYPAL_MODE=sandbox
```

> **NUNCA** incluyas estas claves en el código fuente ni en el repositorio de Git. GitHub Secret Scanning está activo y bloqueará cualquier push que contenga claves expuestas.

---

## Instalación y ejecución local

```bash
# 1. Clonar el repositorio
git clone https://github.com/Creyese23/api-dentvision.git
cd api-dentvision

# 2. Configurar base de datos MySQL local
CREATE DATABASE dentvision CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 3. Crear archivo .env con las variables de entorno
cp .env.example .env
# Editar .env con tus credenciales locales

# 4. Compilar y ejecutar
./mvnw spring-boot:run

# La API estará disponible en http://localhost:8080
```

---

## Despliegue en Render

| Campo | Valor |
|---|---|
| Runtime | Java 21 |
| Build command | `./mvnw clean package -DskipTests` |
| Start command | `java -jar target/*.jar` |
| Variables de entorno | Configurar en el panel Environment de Render |

---

## Convenciones de código

- **Entidades:** PascalCase con anotaciones JPA (`@Entity`, `@Table`, `@Column`)
- **Services:** sufijo `Service`, anotados con `@Service @Transactional`
- **Controllers:** sufijo `Controller`, anotados con `@RestController @RequestMapping`
- **DTOs:** sufijo `Request` (entrada) o `Response` (salida)
- **Javadoc:** en métodos públicos de services y controllers

---

## Licencia

Proyecto académico — SENA 2026. Todos los derechos reservados.

---

*DentVision API v2.1 — SENA 2026*
