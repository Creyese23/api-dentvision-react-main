package co.edu.sena.productsreact.security;

// Claims: representación del payload (cuerpo) del JWT con todos sus campos (sub, exp, etc.)
import io.jsonwebtoken.Claims;
// Jwts: clase utilitaria principal de la librería JJWT para crear y parsear tokens JWT
import io.jsonwebtoken.Jwts;
// Decoders: utilidad para decodificar la clave secreta desde formato Base64
import io.jsonwebtoken.io.Decoders;
// Keys: fábrica de claves criptográficas (crea SecretKey a partir de bytes)
import io.jsonwebtoken.security.Keys;
// JwtException: excepción lanzada cuando el JWT es inválido, manipulado o mal formado
import io.jsonwebtoken.JwtException;
// @Value: inyecta valores desde el archivo application.properties/yml
import org.springframework.beans.factory.annotation.Value;
// UserDetails: interfaz de Spring Security que representa al usuario autenticado
import org.springframework.security.core.userdetails.UserDetails;
// @Service: declara esta clase como un servicio de Spring (bean gestionado por el contenedor)
import org.springframework.stereotype.Service;

// SecretKey: tipo de clave criptográfica simétrica para HMAC-SHA256
import javax.crypto.SecretKey;
// Date: tipo usado por la librería JJWT para las fechas de emisión y expiración
import java.util.Date;
// HashMap: mapa de clave-valor para los claims adicionales del JWT (ej: rol del usuario)
import java.util.HashMap;
// Map: interfaz del mapa de claims adicionales
import java.util.Map;
// Function: interfaz funcional para extraer un campo específico del payload del token
import java.util.function.Function;

// @Service: Spring crea y gestiona una única instancia de esta clase (singleton)
@Service
public class JwtService {

    // Clave criptográfica HMAC-SHA256 derivada del secreto configurado en application.properties
    // Se calcula una vez en el constructor y se reutiliza en cada operación
    private final SecretKey secretKey;

    // Tiempo de vida del access token en milisegundos (ej: 3600000 = 1 hora)
    // Se inyecta desde la propiedad app.jwt.expiration-ms del archivo de configuración
    private final long expirationMs;

    // Constructor: Spring inyecta los dos valores desde application.properties automáticamente
    public JwtService(
            // Secreto Base64 configurado en app.jwt.secret (debe ser de al menos 256 bits para HS256)
            @Value("${app.jwt.secret}") String secret,
            // Tiempo de expiración en milisegundos configurado en app.jwt.expiration-ms
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        // Decodifica el secreto de Base64 a un arreglo de bytes
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        // Crea la clave HMAC-SHA256 a partir de los bytes decodificados
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        // Guarda el tiempo de expiración para usarlo al generar tokens
        this.expirationMs = expirationMs;
    }

    // Genera un JWT firmado para el usuario autenticado
    // Dirección: SALIDA hacia el cliente (se envía en la respuesta de login)
    public String generateToken(UserDetails userDetails) {
        // Mapa para los claims adicionales que se incluirán en el payload del JWT
        Map<String, Object> claims = new HashMap<>();

        // Extrae el primer authority (rol) del usuario y lo agrega como claim "role" en el JWT
        // Esto permite que el frontend y otros servicios conozcan el rol sin consultar la BD
        userDetails.getAuthorities().stream()
                .findFirst()
                .ifPresent(auth -> claims.put("role", auth.getAuthority()));

        // Construye el JWT con el patrón builder de JJWT
        return Jwts.builder()
                // Agrega los claims adicionales (ej: {"role": "ROLE_ADMIN"}) al payload
                .claims(claims)
                // subject: el identificador principal del token, en este caso el email del usuario
                .subject(userDetails.getUsername())
                // issuedAt: marca de tiempo de cuando se emitió el token (fecha actual)
                .issuedAt(new Date())
                // expiration: fecha y hora de vencimiento del token (ahora + expirationMs)
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                // Firma el token con la clave secreta usando el algoritmo HMAC-SHA256
                .signWith(secretKey, Jwts.SIG.HS256)
                // Genera la cadena final del JWT (header.payload.signature en Base64URL)
                .compact();
    }

    // Extrae el "subject" (email del usuario) del payload del JWT
    // Es el identificador principal almacenado en el claim "sub" del token
    public String extractUsername(String token) {
        // Claims::getSubject es una referencia al método que obtiene el campo "sub" del payload
        return extractClaim(token, Claims::getSubject);
    }

    // Valida si un JWT es auténtico y no ha vencido para el usuario dado
    // Retorna true si el token es válido, false si fue manipulado, expiró o es de otro usuario
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            // Extrae el username (email) del token
            final String username = extractUsername(token);
            // El token es válido si: el email coincide con el usuario Y el token no está expirado
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Si la firma es inválida, el token fue manipulado, o está mal formado, retorna false
            return false;
        }
    }

    // Verifica si la fecha de expiración del token ya pasó
    // Retorna true si el token ya venció (antes de la fecha actual)
    private boolean isTokenExpired(String token) {
        // Claims::getExpiration obtiene la fecha de expiración del claim "exp"
        // .before(new Date()): true si la expiración ya ocurrió antes del momento actual
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // Método genérico para extraer cualquier claim del payload del JWT
    // T es el tipo del valor a extraer (String, Date, etc.)
    // resolver es la función que indica qué campo extraer del objeto Claims
    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        // Crea un parser de JWT configurado con la clave secreta para verificar la firma
        Claims claims = Jwts.parser()
                // verifyWith: configura la clave que se usará para verificar la firma del token
                .verifyWith(secretKey)
                // build(): construye el parser listo para usar
                .build()
                // parseSignedClaims: parsea y valida el JWT; lanza JwtException si es inválido
                .parseSignedClaims(token)
                // getPayload(): obtiene el objeto Claims con todos los campos del payload
                .getPayload();

        // Aplica la función resolver para extraer el campo específico del payload
        return resolver.apply(claims);
    }
}
