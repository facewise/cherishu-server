package cherish.backend.common.service;

import cherish.backend.auth.jwt.JwtTokenProvider;
import cherish.backend.auth.jwt.TokenInfo;
import cherish.backend.member.service.CustomUserDetailService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;

@Slf4j
@RequiredArgsConstructor
@Service
public class TokenRefreshService {

    public static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";
    public static final String REFRESH_TOKEN_EXPIRED = "Refresh token has been expired. Please log in.";

    private final CustomUserDetailService customUserDetailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final Key key;

    public TokenInfo refreshToken(String refreshToken) {
        // 토큰 payload에서 email 꺼냄
        String emailFromToken = getEmailFromToken(refreshToken);
        // redis에 토큰이 없으면 만료 처리
        if (!redisService.hasRefreshTokenKey(emailFromToken)) {
            throw new JwtException(REFRESH_TOKEN_EXPIRED);
        }
        // redis에서 email로 토큰 꺼냄
        String tokenFromRedis = redisService.getRefreshToken(emailFromToken);

        if (!tokenFromRedis.equals(refreshToken)) {
            throw new JwtException(INVALID_REFRESH_TOKEN);
        }

        UserDetails userDetails = customUserDetailService.loadUserByUsername(emailFromToken);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
        // 기존 refresh token redis에서 삭제
        redisService.deleteRefreshTokenKey(emailFromToken);

        return jwtTokenProvider.generateToken(authentication);
    }

    private String getEmailFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            throw new JwtException(INVALID_REFRESH_TOKEN);
        } catch (ExpiredJwtException e) {
            throw new JwtException(REFRESH_TOKEN_EXPIRED);
        }
    }
}
