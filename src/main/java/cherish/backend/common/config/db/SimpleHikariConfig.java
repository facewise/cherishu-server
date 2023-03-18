package cherish.backend.common.config.db;

import lombok.Data;
import org.springframework.context.annotation.Profile;


@Data
public class SimpleHikariConfig {
    private String username;
    private String password;
    private String jdbcUrl;
    private String driverClassName;
    private String schema;
}
