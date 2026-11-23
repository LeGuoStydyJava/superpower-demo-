# 手机号登录注册 API 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 Java + Spring Boot 3 实现一个手机号注册、登录、返回 JWT 的后端 REST API。

**Architecture:** 标准 Spring 分层（controller → service → repository → entity）。密码用 BCrypt 哈希存储，登录态用手写 JWT 过滤器（`OncePerRequestFilter`）实现，不上整套 Spring Security。数据层用 H2（内存模式），结构上可切 MySQL。

**Tech Stack:** Java 17、Spring Boot 3.3.5、Maven、Spring Data JPA、H2、jjwt 0.12.6、spring-security-crypto（仅 BCrypt）、JUnit 5 + MockMvc + Mockito。

**Spec:** `docs/superpowers/specs/2026-11-24-phone-auth-api-design.md`

## Global Constraints

- Java 17；Spring Boot 3.3.5（parent pom）
- 包名根：`com.example.phoneauth`
- 手机号格式：`^1\d{10}$`（11 位、1 开头）
- 密码长度：6–32 位
- 密码只用 BCrypt 存储，任何 DTO / 响应 / 日志不得出现密码明文或哈希
- JWT 用 HS256，密钥与过期时间放 `application.yml`（`app.jwt.secret` / `app.jwt.expiration-ms`，默认 86400000 = 24h）
- 统一错误体：`ApiError(String message)`
- 状态码约定：注册成功 201、重复注册 409、登录成功 200、密码错/账号不存在/令牌无效 401、参数校验失败 400、资源不存在 404、未捕获 500
- 删除仓库内旧 Python 文件 `main.py`

## Review Focus

1. **登录失败不得泄露账号是否存在** — 手机号不存在与密码错误都必须返回 401 与同样文案（「手机号或密码错误」）。
2. **JWT 过期或篡改必须返回 401 而非 500**。
3. **手机号格式非法**（10 位、非 1 开头、含字母）必须返回 400，不得 500 或静默通过。
4. **重复注册同一手机号返回 409，不得覆盖已有用户**。
5. **密码绝不落明文/哈希到任何响应或日志**。

---

### Task 1: 项目脚手架与启动

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/example/phoneauth/PhoneAuthApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/test/java/com/example/phoneauth/PhoneAuthApplicationTests.java`
- Delete: `main.py`

**Interfaces:**
- Produces: 可启动的 Spring Boot 应用、`PhoneAuthApplication`、H2 数据源配置（供后续所有任务使用）

**pom.xml 依赖（version 由 parent 管理，除 jjwt）:**
- `spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-validation`
- `com.h2database:h2`（runtime）
- `org.springframework.security:spring-security-crypto`（仅 BCrypt，无 starter）
- `io.jsonwebtoken:jjwt-api:0.12.6`、`jjwt-impl:0.12.6`（runtime）、`jjwt-jackson:0.12.6`（runtime）
- `spring-boot-starter-test`（test）

**application.yml 关键项:**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:phoneauth;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  jpa:
    hibernate.ddl-auto: update
app:
  jwt:
    secret: "a-super-secret-key-for-hs256-at-least-32-bytes-long!!"
    expiration-ms: 86400000
```

- [ ] **Step 1: 写启动测试（先失败——主类缺失，编译不过）**

```java
@SpringBootTest
class PhoneAuthApplicationTests {
    @Test
    void contextLoads() {}
}
```

- [ ] **Step 2: 运行确认失败**

Run: `mvn -q test`
Expected: 编译失败（`PhoneAuthApplication` 不存在）

- [ ] **Step 3: 实现 `pom.xml`、`PhoneAuthApplication`（`@SpringBootApplication`）与 `application.yml`，删除 `main.py`**

`PhoneAuthApplication` 为带 `main(String[])` 的标准启动类。

- [ ] **Step 4: 运行确认通过**

Run: `mvn -q test`
Expected: BUILD SUCCESS，`contextLoads` 通过

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: scaffold Spring Boot project, drop Python demo"
```

---

### Task 2: User 实体与 UserRepository

**Files:**
- Create: `src/main/java/com/example/phoneauth/entity/User.java`
- Create: `src/main/java/com/example/phoneauth/repository/UserRepository.java`
- Test: `src/test/java/com/example/phoneauth/repository/UserRepositoryTest.java`

**Interfaces:**
- Produces: `User` 实体（`Long id`、`String phone`、`String passwordHash`、`LocalDateTime createdAt`，无参构造 + getter/setter，`createdAt` 用 `@PrePersist` 赋 `LocalDateTime.now()`）；`UserRepository extends JpaRepository<User, Long>`，方法 `Optional<User> findByPhone(String phone)`、`boolean existsByPhone(String phone)`

- [ ] **Step 1: 写失败测试**

```java
@DataJpaTest
class UserRepositoryTest {
    @Autowired UserRepository userRepository;

    @Test
    void findByPhoneAndExistsByPhone() {
        User u = new User();
        u.setPhone("13800138000");
        u.setPasswordHash("hashed");
        userRepository.save(u);

        assertTrue(userRepository.existsByPhone("13800138000"));
        assertFalse(userRepository.existsByPhone("13900139000"));
        assertEquals("13800138000", userRepository.findByPhone("13800138000").orElseThrow().getPhone());
        assertNotNull(u.getCreatedAt());
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `mvn -q test -Dtest=UserRepositoryTest`
Expected: 编译失败（`User` / `UserRepository` 不存在）

- [ ] **Step 3: 实现 `User` 实体与 `UserRepository`**

`User` 用 `@Entity @Table(name="users")`；`phone` 加 `@Column(nullable=false, unique=true)`；`createdAt` 用 `@PrePersist` 赋 `LocalDateTime.now()`。

- [ ] **Step 4: 运行确认通过**

Run: `mvn -q test -Dtest=UserRepositoryTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add User entity and repository"
```

---

### Task 3: JwtService

**Files:**
- Create: `src/main/java/com/example/phoneauth/service/JwtService.java`
- Create: `src/main/java/com/example/phoneauth/exception/ApiException.java`
- Test: `src/test/java/com/example/phoneauth/service/JwtServiceTest.java`

**Interfaces:**
- Produces:
  - `ApiException extends RuntimeException`，构造 `ApiException(HttpStatus status, String message)`，带 `getStatus()` 返回 `HttpStatus`
  - `JwtService`：构造 `JwtService(String secret, long expirationMs)`（用 `@Value` 注入）；`String generateToken(Long userId)`；`Long parseUserId(String token)`（无效/过期/篡改时抛 `ApiException(HttpStatus.UNAUTHORIZED, "令牌无效或已过期")`）

- [ ] **Step 1: 写失败测试**

```java
class JwtServiceTest {
    private static final String SECRET = "a-super-secret-key-for-hs256-at-least-32-bytes-long!!";
    private final JwtService jwtService = new JwtService(SECRET, 60000L);

    @Test
    void generateThenParseReturnsSameUserId() {
        String token = jwtService.generateToken(42L);
        assertEquals(42L, jwtService.parseUserId(token));
    }

    @Test
    void parseTamperedTokenThrows401() {
        String token = jwtService.generateToken(42L) + "x";
        ApiException e = assertThrows(ApiException.class, () -> jwtService.parseUserId(token));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void parseExpiredTokenThrows401() {
        JwtService shortLived = new JwtService(SECRET, 1L);
        String token = shortLived.generateToken(42L);
        assertThrows(ApiException.class, () -> shortLived.parseUserId(token));
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `mvn -q test -Dtest=JwtServiceTest`
Expected: 编译失败（`JwtService` / `ApiException` 不存在）

- [ ] **Step 3: 实现 `JwtService` 与 `ApiException`**

JWT 用 jjwt 0.12：`Keys.hmacShaKeyFor(secret.getBytes(UTF_8))`；`Jwts.builder().subject(String.valueOf(userId)).issuedAt(now).expiration(now + expirationMs).signWith(key).compact()`；解析用 `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`，subject 转 `Long`。`ApiException` 的 `getStatus()` 命名注意与字段不冲突。

- [ ] **Step 4: 运行确认通过**

Run: `mvn -q test -Dtest=JwtServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add JwtService and ApiException"
```

---

### Task 4: 注册功能

**Files:**
- Create: `src/main/java/com/example/phoneauth/dto/RegisterRequest.java`
- Create: `src/main/java/com/example/phoneauth/dto/UserResponse.java`
- Create: `src/main/java/com/example/phoneauth/config/AppConfig.java`
- Create: `src/main/java/com/example/phoneauth/service/AuthService.java`
- Test: `src/test/java/com/example/phoneauth/service/AuthServiceTest.java`

**Interfaces:**
- Consumes: `UserRepository`（Task 2）、`JwtService`（Task 3）、`ApiException`（Task 3）
- Produces:
  - `RegisterRequest(String phone, String password)` — record，`phone` 加 `@NotBlank` + `@Pattern(regexp="^1\\d{10}$")`，`password` 加 `@NotBlank` + `@Size(min=6, max=32)`
  - `UserResponse(Long id, String phone, LocalDateTime createdAt)` — record，带静态 `from(User u)`
  - `AppConfig`：`@Bean PasswordEncoder passwordEncoder()` 返回 `new BCryptPasswordEncoder()`
  - `AuthService(UserRepository, PasswordEncoder, JwtService)`，方法 `UserResponse register(RegisterRequest request)`：手机号已存在抛 `ApiException(409, "该手机号已注册")`；否则 BCrypt 加密密码、保存、返回 `UserResponse.from(saved)`

- [ ] **Step 1: 写失败测试**

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    @Test
    void registerHashesPasswordAndReturnsUserResponse() {
        RegisterRequest req = new RegisterRequest("13800138000", "secret123");
        when(userRepository.existsByPhone("13800138000")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = authService.register(req);

        assertEquals("13800138000", resp.phone());
        assertNotNull(resp.id());
        verify(passwordEncoder).encode("secret123");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("hashed", captor.getValue().getPasswordHash());
    }

    @Test
    void registerDuplicatePhoneThrows409() {
        RegisterRequest req = new RegisterRequest("13800138000", "secret123");
        when(userRepository.existsByPhone("13800138000")).thenReturn(true);
        ApiException e = assertThrows(ApiException.class, () -> authService.register(req));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `mvn -q test -Dtest=AuthServiceTest`
Expected: 编译失败（DTO / `AuthService` / `AppConfig` 不存在）

- [ ] **Step 3: 实现上述四个类**

`register` 里手机号存在性用 `existsByPhone` 判断；保存后返回 `UserResponse.from(saved)`。

- [ ] **Step 4: 运行确认通过**

Run: `mvn -q test -Dtest=AuthServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add register with BCrypt hashing"
```

---

### Task 5: 登录功能

**Files:**
- Create: `src/main/java/com/example/phoneauth/dto/LoginRequest.java`
- Create: `src/main/java/com/example/phoneauth/dto/AuthResponse.java`
- Modify: `src/main/java/com/example/phoneauth/service/AuthService.java`
- Test: `src/test/java/com/example/phoneauth/service/AuthServiceLoginTest.java`

**Interfaces:**
- Consumes: `UserRepository`、`PasswordEncoder`、`JwtService`、`ApiException`、`UserResponse`（Task 4）
- Produces:
  - `LoginRequest(String phone, String password)` — record，两字段均 `@NotBlank`
  - `AuthResponse(String token, UserResponse user)` — record
  - `AuthService` 新增方法 `AuthResponse login(LoginRequest request)`：`findByPhone` 找不到，或 `passwordEncoder.matches` 失败，都抛 `ApiException(401, "手机号或密码错误")`；成功则 `generateToken(user.getId())` 并返回 `new AuthResponse(token, UserResponse.from(user))`

- [ ] **Step 1: 写失败测试**

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    private User user() {
        User u = new User();
        u.setId(7L);
        u.setPhone("13800138000");
        u.setPasswordHash("hashed");
        return u;
    }

    @Test
    void loginSuccessReturnsTokenAndUser() {
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(7L)).thenReturn("jwt-token");

        AuthResponse resp = authService.login(new LoginRequest("13800138000", "secret123"));

        assertEquals("jwt-token", resp.token());
        assertEquals("13800138000", resp.user().phone());
    }

    @Test
    void loginWrongPasswordThrows401() {
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        ApiException e = assertThrows(ApiException.class,
            () -> authService.login(new LoginRequest("13800138000", "bad")));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void loginUnknownPhoneThrows401() {
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.empty());
        ApiException e = assertThrows(ApiException.class,
            () -> authService.login(new LoginRequest("13800138000", "secret123")));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `mvn -q test -Dtest=AuthServiceLoginTest`
Expected: 编译失败（`login` 方法 / `LoginRequest` / `AuthResponse` 不存在）

- [ ] **Step 3: 实现 `LoginRequest`、`AuthResponse` 及 `AuthService.login`**

- [ ] **Step 4: 运行确认通过**

Run: `mvn -q test -Dtest=AuthServiceLoginTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add login returning JWT"
```

---

### Task 6: 鉴权过滤器与「当前用户」接口

**Files:**
- Create: `src/main/java/com/example/phoneauth/security/JwtAuthFilter.java`
- Create: `src/main/java/com/example/phoneauth/config/FilterConfig.java`
- Create: `src/main/java/com/example/phoneauth/service/UserService.java`
- Create: `src/main/java/com/example/phoneauth/controller/UserController.java`
- Test: `src/test/java/com/example/phoneauth/controller/UserControllerTest.java`

**Interfaces:**
- Consumes: `JwtService`（Task 3）、`ApiException`（Task 3）、`UserRepository`（Task 2）、`UserResponse`（Task 4）
- Produces:
  - `JwtAuthFilter extends OncePerRequestFilter`，构造 `JwtAuthFilter(JwtService, ObjectMapper)`；`doFilterInternal`：取 `Authorization` 头，无或非 `Bearer ` 前缀 → 写 401 `ApiError("未登录")`；否则 `parseUserId` 成功则 `request.setAttribute("userId", userId)` 并放行，`ApiException` 则写 401
  - `FilterConfig`：`FilterRegistrationBean<JwtAuthFilter>`，`addUrlPatterns("/api/users/*")`
  - `UserService(UserRepository)`，方法 `UserResponse getById(Long id)`：`findById` 找不到抛 `ApiException(404, "用户不存在")`
  - `UserController`：`@RequestMapping("/api/users")`，`@GetMapping("/me")`，参数 `@RequestAttribute("userId") Long userId`，返回 `userService.getById(userId)`

- [ ] **Step 1: 写失败测试（MockMvc 单测控制器层）**

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;

    @Test
    void meReturnsCurrentUser() throws Exception {
        when(userService.getById(7L)).thenReturn(new UserResponse(7L, "13800138000", LocalDateTime.now()));

        mockMvc.perform(get("/api/users/me").requestAttr("userId", 7L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phone").value("13800138000"));
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `mvn -q test -Dtest=UserControllerTest`
Expected: 编译失败（`UserController` / `UserService` 不存在）

- [ ] **Step 3: 实现 `JwtAuthFilter`、`FilterConfig`、`UserService`、`UserController`**

过滤器写 401 用 `objectMapper.writeValue(response.getWriter(), new ApiError(...))` 并 `response.setStatus(401)`、`setContentType("application/json")`。`ApiError` 记录若尚未创建，在此任务一并创建：`src/main/java/com/example/phoneauth/dto/ApiError.java`，`record ApiError(String message)`。

- [ ] **Step 4: 运行确认通过**

Run: `mvn -q test -Dtest=UserControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add JWT auth filter and /api/users/me"
```

---

### Task 7: 认证控制器、全局异常与集成测试

**Files:**
- Create: `src/main/java/com/example/phoneauth/controller/AuthController.java`
- Create: `src/main/java/com/example/phoneauth/exception/GlobalExceptionHandler.java`
- Create: `src/test/java/com/example/phoneauth/AuthFlowIntegrationTest.java`

**Interfaces:**
- Consumes: `AuthService`（Task 4/5）、`UserResponse`、`AuthResponse`、`ApiException`、`ApiError`、`JwtService`/过滤器（Task 6）
- Produces:
  - `AuthController`：`@RequestMapping("/api/auth")`；`@PostMapping("/register")` → `@Valid @RequestBody RegisterRequest`，返回 `ResponseEntity<UserResponse>`（201）；`@PostMapping("/login")` → `@Valid @RequestBody LoginRequest`，返回 `AuthResponse`
  - `GlobalExceptionHandler`（`@RestControllerAdvice`）：`ApiException` → 对应 `status` + `ApiError(message)`；`MethodArgumentNotValidException` → 400 + `ApiError(首条校验消息)`；`Exception` → 500 + `ApiError("服务器内部错误")`

- [ ] **Step 1: 写失败集成测试（覆盖 Review Focus 3、4 及完整闭环）**

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void registerLoginThenMe() throws Exception {
        String body = """
            {"phone":"13800138000","password":"secret123"}
            """;
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.phone").value("13800138000"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String token = JsonPath.read(
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            "$.token");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phone").value("13800138000"));
    }

    @Test
    void duplicateRegisterReturns409() throws Exception {
        String body = """
            {"phone":"13800138000","password":"secret123"}
            """;
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void invalidPhoneFormatReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"123","password":"secret123"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `mvn -q test -Dtest=AuthFlowIntegrationTest`
Expected: 编译失败（`AuthController` / `GlobalExceptionHandler` 不存在）

- [ ] **Step 3: 实现 `AuthController` 与 `GlobalExceptionHandler`**

- [ ] **Step 4: 运行确认通过**

Run: `mvn -q test`
Expected: 全部测试通过、BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add auth endpoints, error handling, integration tests"
```

---

## Self-Review 结果

- **Spec 覆盖**：注册（Task 4/7）、登录（Task 5/7）、`/me` 鉴权（Task 6/7）、手机号格式校验（Task 4 DTO + Task 7）、唯一性校验（Task 4/7）、统一错误返回（Task 7）、BCrypt（Task 4）、JWT（Task 3/6）——均已覆盖。
- **Review Focus 映射**：1→Task 5（密码错/账号不存在同 401 文案）；2→Task 3（篡改/过期 401）；3→Task 7（非法手机号 400）；4→Task 4 + Task 7（重复注册 409）；5→Task 4（响应无密码）+ Task 7（`passwordHash` 不存在断言）。
- **类型一致性**：`ApiException(HttpStatus, String)`、`getStatus()`、`JwtService.generateToken/parseUserId`、`UserResponse.from`、`AuthResponse(token,user)`、`UserService.getById` 在各任务间签名一致。
