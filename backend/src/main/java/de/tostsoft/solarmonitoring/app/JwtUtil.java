package de.tostsoft.solarmonitoring.app;

import de.tostsoft.solarmonitoring.lib.model.JWTSessionToken;
import de.tostsoft.solarmonitoring.lib.model.User;

import de.tostsoft.solarmonitoring.lib.repository.JWTSessionTokenRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtUtil {

  @Autowired
  private JWTSessionTokenRepository jwtSessionTokenRepository;

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

  public String extractId(String token) {
    return extractClaim(token, Claims::getId);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);

    if(claims.getId() == null){
      throw new RuntimeException("Could not extract id from jwt token");
    }

    if(!jwtSessionTokenRepository.existsById(claims.getId())){
      throw new RuntimeException("Token isn't valid any more");
    }

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
    return createJWT(claims, user.getName(), user);
  }

  private String createJWT(Map<String, Object> claims, String name, User user) {

    Instant now = Instant.now().plus(30, ChronoUnit.DAYS);

    LocalDateTime expirationDate = LocalDateTime.ofInstant(now, ZoneId.of("UTC"));

    JWTSessionToken token =  JWTSessionToken.builder()
            .validUntil(expirationDate)
            .ownedBy(user)
            .build();

    token = jwtSessionTokenRepository.save(token);

    return Jwts.builder()
            .subject(name)
            .claims(claims)
            .id(token.getId())
            .issuedAt(new Date())
            .expiration(new Date(now.toEpochMilli()))
            .signWith(secretKey,Jwts.SIG.HS256)
            .compact();
  }

  public Boolean validateToken(String token, User user) {

    final String name = extractUsername(token);
    return (name.equalsIgnoreCase(user.getName()) && !isTokenExpired(token));
  }
}