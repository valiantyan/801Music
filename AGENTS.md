# Kotlin Project Coding Standards (Detailed)

## Language Requirements

- **Code**: All code (variable names, function names, class names, etc.) must be in **English**
- **Comments & Documentation**: All comments and documentation must be in **Chinese**

---    
## Type Declarations

- **Explicitly declare types** for all variables and functions (parameters and return types)
- **Avoid using `any` type**; create specific types when necessary
- **No type inference ambiguity** - make types clear and explicit

---    
## Code Formatting

- **No empty lines inside function bodies**
- **Keep functions concise** - not exceeding 20 instructions
- **Keep classes small** - less than 200 lines, less than 10 public methods, less than 10 properties
- **Follow Single Responsibility Principle** - each file/class should have one clear purpose
- **Maintain single level of abstraction** within functions

---    
## Function Design

- **Name functions using a verb followed by other words** (e.g., `fetchUser`, `validateInput`)
- **Boolean functions**: Use prefixes like `isX`, `hasX`, `canX`
- **Functions without return values**: Use action verbs like `executeX`, `saveX`
- **Avoid nested code blocks** through early checks and returns
- **Use higher-order functions** (map, filter, reduce) to avoid nesting
- **Use arrow functions** for simple functions (< 3 instructions)
- **Use default parameter values** instead of checking for null
- **RO-RO pattern**: Use objects to pass multiple parameters and return results

---    
## Parameter Formatting

- **2+ parameters**: Use named parameters
- **3+ parameters**: Each parameter must be on a separate line
- **Trailing comma**: Always include a trailing comma after the last parameter

### Example

```kotlin  // 2-3 parameters with named arguments  val user = getUser(    
    id = "123456",    includeDetails = true,)    
    
// 4+ parameters, one per line  val config = createConfig(    
name = "AppConfig",    version = "1.0.0",    debug = BuildConfig.DEBUG,    timeout = 30000,)    
```    
    
---    
## Naming Conventions

| Element | Convention | Example |  |---------|-----------|---------|  | Classes | PascalCase | `UserRepository` |  | Interfaces | PascalCase | `DataSource` |  | Variables | camelCase | `userName` |  | Functions | camelCase (start with verb) | `fetchUserProfile` |  | Constants | UPPERCASE | `MAX_RETRY_COUNT` |  | Files | PascalCase | `UserRepository.kt` |  | Directories | underscores_case | `user_repository` |  | Boolean variables | is/has/can prefix | `isLoading`, `hasError` |
    
---    
## Data & Class Design

- **Prefer data classes** for handling data
- **Prefer immutable data**: Use `val` instead of `var`
- **Validate data inside classes**, not in functions
- **Follow SOLID principles**
- **Prefer composition over inheritance**
- **Use interfaces to define contracts**

### Example

```kotlin  // Good data class design  data class User(    
    val id: String,    val username: String,    val email: String,    val createdAt: Long = System.currentTimeMillis())    
    
// Class with internal validation  class Email private constructor(val value: String) {    
    companion object {        fun create(email: String): Result<Email> {            return if (isValidEmail(email)) {                Result.success(Email(email))            } else {                Result.failure(IllegalArgumentException("无效的邮箱格式"))    
}        }    }}    
```    
    
---    
## Exception Handling

- **Log execution progress** within try blocks to track program flow
- **Catch specific exceptions**, not generic `Exception`
- **Add contextual information** when catching exceptions
- **Never swallow exceptions** - always log or rethrow
- **Use Result type** or sealed classes for error states

### Example

```kotlin  fun fetchUserData(userId: String): User {    
    try {        Log.d(TAG, "开始获取用户数据: userId=$userId")    
        val response = apiService.getUser(userId)        Log.d(TAG, "获取用户数据成功: ${response.name}")    
        return response    } catch (e: ApiException) {        Log.e(TAG, "API调用失败: ${e.message}", e)    
        throw UserDataException("无法获取用户数据", e)    
    } catch (e: NetworkException) {        Log.e(TAG, "网络连接失败: ${e.message}", e)    
        throw UserDataException("网络连接问题", e)    
}}    
```    
    
---    
## Documentation & Comments

- **Public APIs must have complete KDoc documentation**
- **Comments should explain "why"**, not just "what"
- **Keep comments synchronized** with code changes
- **Use square brackets `[]`** to reference classes, methods, and fields
- **Document constants individually** at their definition point
- **Critical private methods** (>30 lines or calling >5 methods in key classes >200 lines) must have detailed comments

### KDoc Format

```kotlin  /**    
* 根据用户ID获取用户信息    
* * 该方法首先尝试从缓存获取数据，如果缓存不存在或已过期，    
* 则从远程API获取数据并更新缓存    
* * @param userId 要查询的用户ID，不能为空    
* @return 用户信息对象    
* @throws UserNotFoundException 当用户不存在时抛出    
* @throws NetworkException 当网络连接失败时抛出    
 * @see CacheManager.getUser */fun fetchUserById(userId: String): User {    
// Implementation...}    
```    
### Constant Documentation

```kotlin  /**    
* 默认总超时时间(5000ms)    
 */private const val DEFAULT_GLOBAL_TIMEOUT = 5000L    
    
/**    
* 广告源类型    
 * * - [TYPE_HB] Header Bidding广告类型("HB")    
 * - [TYPE_WF] Waterfall广告类型("WF")    
* - [TYPE_FILL] 兜底广告类型("FILL")    
 */data class AdSourceConfig(    
val type: String,)    
```    
    
---    
## Testing Guidelines

- **Follow Arrange-Act-Assert structure**
- **Descriptive test names**: `methodName_condition_expectedResult`
- **Test variable naming**: `inputX`, `mockX`, `actualX`, `expectedX`
- **One scenario per test**
- **Use Test Doubles** (Mocks, Stubs) to isolate code under test
- **Keep tests independent** - no dependency on execution order

### Example

```kotlin  @Test  fun fetchUserById_validUserId_returnsUser() {    
// Arrange - 准备测试数据    
    val expectedUser = User("123", "测试用户", "test@example.com")    
    `when`(mockApiService.getUser("123")).thenReturn(expectedUser)    
    
// Act - 执行被测方法    
    val actualUser = userRepository.fetchUserById("123")    
    
// Assert - 验证结果    
    assertEquals(expectedUser.id, actualUser.id)    
    assertEquals(expectedUser.name, actualUser.name)    
    verify(mockApiService).getUser("123")  }  ```    
    
---    
## Workflow  
  
- **Test after each meaningful change**  
- **Implement core functionality before optimization**  
- **Write concise, focused functions and classes**

---    
## Recent Updates

- 将 `AudioScanIntegrationTest.kt` 从 `androidTest` 移至 `test` 目录，并改为 Robolectric 运行方式
- UT 用例名统一为 **反引号 + 中文命名**（参考 `ScanProgressTest` 风格）
