package de.tostsoft.solarmonitoring;


import de.tostsoft.solarmonitoring.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtUtil {

  @Value("${environment.jwtKey}")
  private String SECRET_KEY;

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
    return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
  }

  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  public String generateToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    return createToken(claims, user.getName(), "" + user.getId());
  }

  private String createToken(Map<String, Object> claims, String name, String id) {
    return Jwts.builder().setClaims(claims).setSubject(name).setId(id).setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() * 1000 * 60 * 60 * 10))
        .signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact();
  }

  public Boolean validateToken(String token, User user) {
    final String name = extractUsername(token);
    return (name.equals(user.getName()) && !isTokenExpired(token));
  }
}