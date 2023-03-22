package cherish.backend.auth.jwt;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TokenInfo {

    private String grantType;
    private String accessToken;
    private String refreshToken;
}
