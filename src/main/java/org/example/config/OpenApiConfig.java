package org.example.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) Configuration
 * Provides interactive API documentation at /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.version:2.0.0}")
    private String appVersion;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public OpenAPI customOpenAPI() {
        // Security scheme for JWT Bearer token
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("Enter JWT token obtained from /api/auth/login endpoint");

        // Security requirement to apply globally
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .externalDocs(externalDocumentation())
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement); // Apply JWT auth globally
    }

    private Info apiInfo() {
        return new Info()
                .title("CRUD Test Application API")
                .description("""
                        ## Enterprise-Grade CRUD Application with JWT Authentication

                        ### Features
                        - **User Management**: Complete CRUD operations for user accounts
                        - **JWT Authentication**: Secure token-based authentication
                        - **Role-Based Access Control**: Admin and User roles with different permissions
                        - **Account Security**: Password hashing with BCrypt (strength 12)
                        - **Input Validation**: Comprehensive validation on all endpoints

                        ### Authentication Flow
                        1. **Register**: Create a new account via `POST /api/auth/register`
                        2. **Login**: Obtain JWT token via `POST /api/auth/login`
                        3. **Authenticate**: Include token in Authorization header as `Bearer {token}`
                        4. **Access Protected Routes**: Use token for all protected endpoints

                        ### Authorization Levels
                        - **Public**: Registration, Login, Health Check
                        - **User**: View/update/delete own profile
                        - **Admin**: Full access to all user management operations

                        ### Error Handling
                        All errors follow a consistent format with timestamp, status, error type, message, and details.

                        ### Default Admin Account (Development)
                        - **Username**: admin
                        - **Password**: admin123
                        - **Email**: admin@crudtest.com
                        - **Role**: ROLE_ADMIN

                        ⚠️ **Important**: Change default credentials in production!
                        """)
                .version(appVersion)
                .contact(apiContact())
                .license(apiLicense());
    }

    private Contact apiContact() {
        return new Contact()
                .name("CRUD Test Application Team")
                .email("support@crudtest.com")
                .url("https://github.com/yourusername/crud-test");
    }

    private License apiLicense() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    private List<Server> serverList() {
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local Development Server");

        Server stagingServer = new Server()
                .url("https://staging.crudtest.com")
                .description("Staging Server");

        Server productionServer = new Server()
                .url("https://api.crudtest.com")
                .description("Production Server");

        // Return only local server in development, all servers in production
        if ("dev".equals(activeProfile)) {
            return List.of(localServer);
        } else {
            return List.of(localServer, stagingServer, productionServer);
        }
    }

    private ExternalDocumentation externalDocumentation() {
        return new ExternalDocumentation()
                .description("Full Documentation & GitHub Repository")
                .url("https://github.com/yourusername/crud-test");
    }
}
