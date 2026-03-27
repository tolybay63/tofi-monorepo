package tofi.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

public class JwtUtils {

    private static final String ISSUER = "tofi-platform";

    /**
     * Теперь принимает attrs И secret
     */
    public static String createToken(Map<String, Object> attrs, String secret) {
        Algorithm algorithm = Algorithm.HMAC256(secret); // Используем переданный секрет

        Instant expInstant = Instant.now().plus(8, ChronoUnit.HOURS);
        Date expDate = Date.from(expInstant);
        return JWT.create()
                .withIssuer(ISSUER)
                .withClaim("attrs", attrs)
                .withExpiresAt(expDate)
                .sign(algorithm);
    }

    /**
     * Теперь принимает token И secret
     */
    public static Map<String, Object> decode(String token, String secret) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);

            return jwt.getClaim("attrs").asMap();
        } catch (Exception e) {
            // Если секрет не подошел или токен протух - вернет null
            return null;
        }
    }
}
