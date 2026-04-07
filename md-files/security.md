# Cursor Rules — Security Standards - Appointment Service

## Authentication & Authorization

### JWT-Based Authentication

```
┌─────────────┐     Login      ┌─────────────┐
│   Client    │ ──────────────►│  Auth API   │
│             │◄────────────── │             │
└─────────────┘   JWT Token    └─────────────┘
       │
       │ Authorization: Bearer <token>
       ▼
┌─────────────┐
│  Protected  │
│    APIs     │
└─────────────┘
```

### JWT Payload Structure
```json
{
    "sub": "user@email.com",
    "businessId": 123,
    "userId": 456,
    "role": "BUSINESS_OWNER",
    "iat": 1704448800,
    "exp": 1704535200
}
```

---

## Role-Based Access Control (RBAC)

### Roles
```java
public enum UserRole {
    SYSTEM_ADMIN,    // Platform yöneticisi
    BUSINESS_OWNER,  // İşletme sahibi
    EMPLOYEE         // İşletme çalışanı
}
```

### Permission Matrix

| Endpoint | SYSTEM_ADMIN | BUSINESS_OWNER | EMPLOYEE | Public |
|----------|-------------|----------------|----------|--------|
| POST /businesses | ✅ | ❌ | ❌ | ❌ |
| GET /businesses/{id} | ✅ | ✅ (own) | ✅ (own) | ❌ |
| POST /employees | ✅ | ✅ (own) | ❌ | ❌ |
| GET /appointments | ✅ | ✅ (own) | ✅ (own) | ❌ |
| POST /appointments/{id}/confirm | ✅ | ✅ (own) | ✅ (own) | ❌ |
| GET /public/businesses/{slug} | ✅ | ✅ | ✅ | ✅ |
| POST /public/appointments | ✅ | ✅ | ✅ | ✅ |

---

## Security Configuration

### Spring Security Config
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/api/v1/webhooks/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("SYSTEM_ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "https://app.appointment-service.com",
            "http://localhost:3000"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

---

## Business-Level Security

### Data Isolation
Her işletme sadece kendi verilerine erişebilir.

```java
@Service
public class AppointmentService {

    public List<Appointment> findByBusinessId(Long businessId) {
        Long currentBusinessId = SecurityContextUtils.getCurrentBusinessId();
        
        if (!currentBusinessId.equals(businessId)) {
            throw new AccessDeniedException("Bu işletmenin verilerine erişim yetkiniz yok");
        }
        
        return repository.findByBusinessId(businessId);
    }
    
    @PreAuthorize("@securityService.hasAccessToAppointment(#appointmentId)")
    public Appointment confirmAppointment(Long appointmentId) {
        // ...
    }
}
```

### Security Service
```java
@Service
public class SecurityService {

    private final AppointmentRepository appointmentRepository;
    
    public boolean hasAccessToAppointment(Long appointmentId) {
        Long currentBusinessId = SecurityContextUtils.getCurrentBusinessId();
        return appointmentRepository.existsByIdAndBusinessId(appointmentId, currentBusinessId);
    }
    
    public boolean hasAccessToCustomer(Long customerId) {
        Long currentBusinessId = SecurityContextUtils.getCurrentBusinessId();
        return customerRepository.existsByIdAndBusinessId(customerId, currentBusinessId);
    }
}
```

---

## Password Security

### Password Policy
```java
public class PasswordValidator {
    
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    
    public void validate(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password.length() < MIN_LENGTH) {
            errors.add("Şifre en az " + MIN_LENGTH + " karakter olmalı");
        }
        if (password.length() > MAX_LENGTH) {
            errors.add("Şifre en fazla " + MAX_LENGTH + " karakter olabilir");
        }
        if (!UPPERCASE.matcher(password).find()) {
            errors.add("Şifre en az bir büyük harf içermeli");
        }
        if (!LOWERCASE.matcher(password).find()) {
            errors.add("Şifre en az bir küçük harf içermeli");
        }
        if (!DIGIT.matcher(password).find()) {
            errors.add("Şifre en az bir rakam içermeli");
        }
        
        if (!errors.isEmpty()) {
            throw new PasswordValidationException(errors);
        }
    }
}
```

### Password Encoding
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

---

## Input Validation & Sanitization

### XSS Prevention
```java
public class InputSanitizer {
    
    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
        .toFactory();
    
    public String sanitize(String input) {
        if (input == null) return null;
        return POLICY.sanitize(input);
    }
}
```

### SQL Injection Prevention
- JPA/Hibernate parameterized queries kullan
- Native query kullanma, kullanılacaksa @Param kullan

```java
// ✅ Doğru: Parameterized query
@Query("SELECT a FROM Appointment a WHERE a.businessId = :businessId AND a.status = :status")
List<Appointment> findByBusinessIdAndStatus(
    @Param("businessId") Long businessId, 
    @Param("status") AppointmentStatus status
);

// ❌ Yanlış: String concatenation
@Query("SELECT a FROM Appointment a WHERE a.businessId = " + businessId)
```

### Request Validation
```java
public record CreateAppointmentRequest(
    @NotNull(message = "Müşteri ID zorunlu")
    Long customerId,
    
    @NotNull @Future(message = "Randevu tarihi gelecekte olmalı")
    LocalDateTime scheduledAt,
    
    @Size(max = 500, message = "Not en fazla 500 karakter")
    @SafeHtml
    String notes
) {}
```

---

## API Security

### Rate Limiting
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter publicLimiter = RateLimiter.create(60.0 / 60);
    private final ConcurrentMap<Long, RateLimiter> userLimiters = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
            HttpServletResponse response, FilterChain chain) {
        
        if (isPublicEndpoint(request)) {
            if (!publicLimiter.tryAcquire()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                return;
            }
        } else {
            Long userId = getCurrentUserId();
            RateLimiter limiter = userLimiters.computeIfAbsent(
                userId, id -> RateLimiter.create(300.0 / 60)
            );
            if (!limiter.tryAcquire()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}
```

### Request Size Limits
```yaml
# application.yml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB

server:
  tomcat:
    max-http-post-size: 10MB
```

---

## Webhook Security

### WhatsApp Webhook Verification
```java
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final String webhookSecret;
    
    @PostMapping("/whatsapp")
    public ResponseEntity<Void> handleWhatsAppWebhook(
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestBody String payload) {
        
        if (!verifySignature(payload, signature)) {
            log.warn("Invalid webhook signature received");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        webhookHandler.process(payload);
        return ResponseEntity.ok().build();
    }
    
    private boolean verifySignature(String payload, String signature) {
        String expected = "sha256=" + HmacUtils.hmacSha256Hex(webhookSecret, payload);
        return MessageDigest.isEqual(expected.getBytes(), signature.getBytes());
    }
}
```

### IP Whitelisting
```java
@Component
public class WebhookIpFilter extends OncePerRequestFilter {

    private static final Set<String> WHATSAPP_IPS = Set.of(
        "185.60.216.0/22",
        "185.60.220.0/22"
        // Meta'nın IP aralıkları
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
            HttpServletResponse response, FilterChain chain) {
        
        if (request.getRequestURI().startsWith("/api/v1/webhooks/")) {
            String clientIp = getClientIp(request);
            if (!isWhitelisted(clientIp)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}
```

---

## Sensitive Data Handling

### Data Masking
```java
public class DataMaskingUtils {
    
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
    }
    
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String masked = parts[0].charAt(0) + "***";
        return masked + "@" + parts[1];
    }
}

// Log'larda kullanım
log.info("Randevu oluşturuldu: musteri={}", 
    DataMaskingUtils.maskPhoneNumber(customer.getPhone()));
```

### Audit Logging
```java
@Aspect
@Component
public class AuditAspect {

    @AfterReturning("@annotation(Auditable)")
    public void audit(JoinPoint joinPoint) {
        AuditLog log = AuditLog.builder()
            .userId(SecurityContextUtils.getCurrentUserId())
            .businessId(SecurityContextUtils.getCurrentBusinessId())
            .action(joinPoint.getSignature().getName())
            .timestamp(LocalDateTime.now())
            .build();
        
        auditRepository.save(log);
    }
}

@Auditable
public Appointment confirmAppointment(Long appointmentId) {
    // ...
}
```

---

## Security Headers

```java
@Configuration
public class SecurityHeadersConfig {

    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registration = 
            new FilterRegistrationBean<>();
        registration.setFilter(new SecurityHeadersFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }
}

public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
            HttpServletResponse response, FilterChain chain) {
        
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Strict-Transport-Security", 
            "max-age=31536000; includeSubDomains");
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'");
        
        chain.doFilter(request, response);
    }
}
```

---

## Security Checklist

### Development
- [ ] HTTPS kullanımı (production'da zorunlu)
- [ ] Güçlü şifre politikası
- [ ] JWT token expiration (24 saat)
- [ ] Input validation tüm endpoint'lerde
- [ ] SQL injection koruması
- [ ] XSS koruması

### Deployment
- [ ] Secrets environment variable'dan
- [ ] Database credentials güvenli
- [ ] CORS doğru yapılandırılmış
- [ ] Rate limiting aktif
- [ ] Security headers aktif
- [ ] Audit logging aktif
