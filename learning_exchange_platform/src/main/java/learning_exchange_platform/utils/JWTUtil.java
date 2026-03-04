package learning_exchange_platform.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

public class JWTUtil {
    private static final String SECRET_KEY_STRING = "fsf65sd1fsdg64sdg981sd6gsd8gs8dg";//使用固定密钥，防止jwt令牌提前失效
    private static SecretKey secretKey;
    private static Long expirationTime=43200000L;//过期时间为12个小时

    //生成JWT令牌
    public static String generateJWT(Map<String, Object> claims) {
        byte[] keyBytes = SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
        String jwt = Jwts.builder()
                .signWith(secretKey)//签名算法
                .setClaims(claims)//自定义内容
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .compact();
        return jwt;
    }

    //解析JWT令牌
    public static Claims parseJWT(String jwt) throws Exception {
        try {
            String base64Key = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            Claims claims=Jwts.parser()
                    .setSigningKey(base64Key)
                    .parseClaimsJws(jwt)
                    .getBody();
            return claims;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
