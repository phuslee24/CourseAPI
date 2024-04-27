package com.example.demo.Services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.cglib.core.internal.Function;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.example.demo.Services.IRepositoryServices.IJwtRepo;

import java.security.*;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import java.security.spec.ECGenParameterSpec;

@Service
public class JWTService implements  IJwtRepo{
    private KeyPair keyPair;

    @PostConstruct
    public void initKeys() {
        if (this.keyPair == null) {
            try {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
                ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
                keyPairGenerator.initialize(ecSpec, new SecureRandom());
                this.keyPair = keyPairGenerator.generateKeyPair();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to generate EC key pair", e);
            }
        }
    }

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))  // 24 hours
                .signWith(getPrivateKey(), SignatureAlgorithm.ES256)
                .compact();
    }

    public String generateRefreshToken(Map<String, Objects> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))  // 24 hours
                .signWith(getPrivateKey(), SignatureAlgorithm.ES256)
                .compact();
    }

    public String extractUserName(String token) {
        return extracClaim(token, Claims::getSubject);
    }

    private <T> T extracClaim(String token, Function<Claims, T> claimsTFunction) {
        final Claims claims = extractAllClaims(token);
        return claimsTFunction.apply(claims);
    }

//    private PrivateKey getSignKey() {
//        try {
//            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
//
//            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
//
//            keyPairGenerator.initialize(ecSpec, new SecureRandom());
//
//            KeyPair keyPair = keyPairGenerator.generateKeyPair();
//
//            return keyPair.getPrivate();
//        } catch (Exception e) {
//            throw new IllegalStateException("Error generating ECDSA key", e);
//        }
//    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getPublicKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private PrivateKey getPrivateKey() {
        return this.keyPair.getPrivate();
    }

    private PublicKey getPublicKey() {
        return this.keyPair.getPublic();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUserName(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExprired(token));
    }

    private boolean isTokenExprired(String token) {
        return extracClaim(token, Claims::getExpiration).before(new Date());
    }
}
