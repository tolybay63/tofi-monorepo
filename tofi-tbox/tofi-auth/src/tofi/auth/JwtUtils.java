package tofi.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class JwtUtils {

    private static final String ISSUER = "tofi-platform";

    /**
     * 1. Короткий Access Token (15 минут) с полным набором атрибутов пользователя
     */
    public static String createAccessToken(Map<String, Object> attrs, String secret) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        Instant expInstant = Instant.now().plus(15, java.time.temporal.ChronoUnit.MINUTES);
        //Date expDate = Date.from(expInstant);

        return JWT.create()
                .withIssuer(ISSUER)
                .withClaim("type", "access")
                .withClaim("attrs", attrs)
                .withExpiresAt(expInstant)
                .sign(algorithm);
    }

    /**
     * 2. Длинный Refresh Token (7 дней) — содержит только ID пользователя
     */
    public static String createRefreshToken(Object userId, String secret) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        Instant expInstant = Instant.now().plus(7, ChronoUnit.DAYS);
        //Date expDate = Date.from(expInstant);

        return JWT.create()
                .withIssuer(ISSUER)
                .withClaim("type", "refresh")
                .withClaim("userId", String.valueOf(userId))
                .withExpiresAt(expInstant)
                .sign(algorithm);
    }

    /**
     * Токен для старых клиентов (legacy) во время переходного периода.
     * Выпускается сразу на 8 часов и не требует Refresh-токена.
     */
    public static String createLegacyToken(Map<String, Object> attrs, String secret) {
        com.auth0.jwt.algorithms.Algorithm algorithm = com.auth0.jwt.algorithms.Algorithm.HMAC256(secret);
        java.time.Instant expInstant = java.time.Instant.now().plus(8, java.time.temporal.ChronoUnit.HOURS);

        return com.auth0.jwt.JWT.create()
                .withIssuer(ISSUER)
                .withClaim("type", "access") // Важно, чтобы старый токен успешно проходил валидацию decode()
                .withClaim("attrs", attrs)
                .withExpiresAt(java.util.Date.from(expInstant))
                .sign(algorithm);
    }

    /**
     * Старый метод декодирования (модифицированный).
     * Проверяет подпись и то, что этот токен именно типа "access"
     */
    public static Map<String, Object> decode(String token, String secret) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            // Строим верификатор, который СТРОГО проверяет время жизни (без погрешностей Leeway)
            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .acceptLeeway(0) // Нам не нужны поблажки в 10-15 минут, проверяем секунда в секунду!
                    .build()
                    .verify(token);

            // Проверяем тип токена, чтобы никто не подсунул рефреш вместо аксесса
            String type = jwt.getClaim("type").asString();
            if (!"access".equals(type)) return null;

            return jwt.getClaim("attrs").asMap();
        } catch (Exception e) {
            // Если токен физически протух, verify(token) выбросит TokenExpiredException,
            // метод вернет null, и TofiSecurityFilter выдаст "lifetime_expired"
            return null;
        }
    }

    /**
     * 3. Специальный декодер для проверки Refresh-токена.
     * Возвращает userId, если токен валиден.
     */
    public static String getUserIdFromRefreshToken(String token, String secret) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);

            String type = jwt.getClaim("type").asString();
            if (!"refresh".equals(type)) return null;

            return jwt.getClaim("userId").asString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 4. Хэширует строку (рефреш-токен) алгоритмом SHA-256 для безопасного хранения в БД
     */
    public static String hashToken(String token) {
        if (token == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при хэшировании токена", e);
        }
    }
}