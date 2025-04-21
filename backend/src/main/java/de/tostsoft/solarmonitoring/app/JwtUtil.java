package de.tostsoft.solarmonitoring.app;

import de.tostsoft.solarmonitoring.lib.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.KeyException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecretKeyBuilder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtUtil {

  @Value("${jwt.key}")
  private String SECRET_KEY;
  private SecretKey secretKey;

  @PostConstruct
  public void init(){
    byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
    secretKey = Keys.hmacShaKeyFor(keyBytes);
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
  }

  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  public String generateJWT(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("admin", user.getIsAdmin());
    return createJWT(claims, user.getName(), user.getId());
  }

  private String createJWT(Map<String, Object> claims, String name, String id) {

    return Jwts.builder()
            .subject(name)
            .claims(claims)
            .id(id)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() * 1000 * 60 * 60 * 10))
            .signWith(secretKey,Jwts.SIG.HS256)
            .compact();
  }

  public Boolean validateToken(String token, User user) {
    final String name = extractUsername(token);
    return (name.equalsIgnoreCase(user.getName()) && !isTokenExpired(token));
  }
}