# Ревью проекта MovieNight Backend

## Покрытие обязательных тем

Обозначения: ✅ — есть вклад / правки; ❌ — нет вклада в репозитории.

| Тема                                                        | skettiks | glashark | devitq |
|-------------------------------------------------------------|:--------:|:--------:|:------:|
| **Скелет проекта** (Spring Boot, gradle, базовые настройки) |    ✅     |    ❌     |   ✅    |
| **Контракты API** (постман / описание запросов)             |    ?     |    ?     |   ❌    |
| **Контроллеры**                                             |    ✅     |    ❌     |   ❌    |
| **Docker**                                                  |    ✅     |    ❌     |   ✅    |
| **SQL** (схема + миграции)                                  |    ✅     |    ✅     |   ❌    |
| **Spring JPA**                                              |    ❌     |    ❌     |   ❌    |
| **Spring Test**                                             |    ❌     |    ❌     |   ❌    |

---

## Замечания

### Замечание 1. InMemory-адаптеры больше не нужны

**Файлы:**

- `src/main/kotlin/com/project/movienight/adapters/persistence/inmemory/FilmRepositoryAdapter.kt`
- `src/main/kotlin/com/project/movienight/adapters/persistence/inmemory/UserRepositoryAdapter.kt`
- `src/main/kotlin/com/project/movienight/adapters/persistence/inmemory/FilmLibraryRepositoryAdapter.kt`

Когда у вас появились реальные репозитории с БД, in-memory адаптеры в `src/main/` стали лишними.

**Но есть один важный нюанс:** новые JDBC-репозитории НЕ реализуют порты `FilmRepositoryPort`/`UserRepositoryPort`/`FilmLibraryRepositoryPort`. Сейчас сервисы (`FilmService`, `UserService`) зависят именно от портов, а единственная реализация портов — это in-memory адаптеры.

---

### Замечание 2. Request/Response DTO лучше выносить в отдельные пакеты

**Файлы:**

- `src/main/kotlin/com/project/movienight/adapters/web/FilmController.kt:50-58`
- `src/main/kotlin/com/project/movienight/adapters/web/UserController.kt:49-56`
- `src/main/kotlin/com/project/movienight/adapters/web/FilmLibraryController.kt:70-72`

Сейчас DTO-классы (`CreateFilmRequest`, `EditFilmRequest`, `CreateUserRequest`, `EditUserRequest`, `CreateFilmLibraryRequest`) объявлены прямо в файлах контроллеров. Пока их по 1-2 на файл — это терпимо. Но как только добавятся новые endpoint'ы, валидация и Response-DTO — контроллер будет нечитаем.

Я могу предложить такую структуру:

```
adapters/web/
├── FilmController.kt
├── UserController.kt
├── FilmLibraryController.kt
└── dto/
    ├── request/
    │   ├── CreateFilmRequest.kt
    │   ├── EditFilmRequest.kt
    │   ├── CreateUserRequest.kt
    │   └── ...
    └── response/
        ├── FilmResponse.kt
        ├── UserResponse.kt
        └── ...
```

`request` — то, что приходит в контроллер (что клиент отправляет вам в теле запроса).
`response` — то, что контроллер отдаёт наружу (см. замечание №3, оно с этим тесно связано).

---

### Замечание 3. Гексагональная архитектура реализована неправильно — domain «течёт» наружу

**Файлы:**

- `src/main/kotlin/com/project/movienight/adapters/web/FilmController.kt:23` — `fun create(...): Film`
- `src/main/kotlin/com/project/movienight/adapters/web/UserController.kt:23` — `fun create(...): User`
- `src/main/kotlin/com/project/movienight/adapters/web/FilmLibraryController.kt:30` — `fun create(...): FilmLibrary`
- (то же касается всех остальных endpoint'ов)

**В чём суть.** В гексагональной архитектуре для вашего проекта есть **три слоя**:

```
┌──────────┐      ┌─────────┐      ┌─────┐
│   REST   │ <--> │ DOMAIN  │ <--> │ DB  │
│ (адаптер)│      │ (ядро)  │      │     │
└──────────┘      └─────────┘      └─────┘
   Request          Domain           DB
   /Response        Models           Entity
```

**Главное правило:** `domain` — это центр. Он **не зависит ни от кого**. `REST` и `DB` — это адаптеры, они зависят от `domain`. И никто извне не должен видеть/возвращать domain-модели наружу — иначе любое изменение в `domain.User` сломает контракт API клиента.

**Что у вас сейчас нарушено:**

1. **REST-слой возвращает доменные модели** (`Film`, `User`, `FilmLibrary`) напрямую в HTTP-ответе. Должен возвращать `FilmResponse`, `UserResponse`, `FilmLibraryResponse` — отдельные DTO для API.
2. **DB-слой работает с доменом без маппинга**. Должны быть JDBC/JPA-сущности (`UserEntity`, `FilmEntity`) и маппер `UserEntity ↔ User` внутри репозитория. Наружу из репозитория всегда уходят domain-модели.

**Как должно быть (на примере User):**

**Главная мысль:** domain-слой — это «правда» о бизнес-сущности. Снаружи (в REST или в БД) у этой сущности могут быть совершенно другие представления — и это нормально. Цель архитектуры — чтобы домен можно было поменять, не ломая никого.

---

### Замечание 4. Repositories не подключены к логике — сделаны «для галочки»

**Файлы:**

- `src/main/kotlin/com/project/movienight/adapters/persistence/FilmRepository.kt:9-10`
- `src/main/kotlin/com/project/movienight/adapters/persistence/UserRepository.kt:9-10`
- `src/main/kotlin/com/project/movienight/adapters/persistence/FilmLibraryRepository.kt:8-9`

1. **Не имплементируют порты** (`FilmRepositoryPort`, `UserRepositoryPort`, `FilmLibraryRepositoryPort`).
2. **Никем не инжектятся.** Ни один сервис на них не ссылается. Spring их создаст как бины — и они никогда не будут вызваны.
3. **Реальная логика всё ещё работает на in-memory адаптерах**.

---

### Замечание 5. Использовать кастомные исключения

**Файлы:**

- `src/main/kotlin/com/project/movienight/application/services/FilmService.kt:25,28,45,48,51,59`
- `src/main/kotlin/com/project/movienight/application/services/UserService.kt:25,43,46,54`

**В чём суть.** Сейчас везде вы кидаете `throw IllegalArgumentException(...)`. Это плохо по двум причинам:

1. Невозможно отличить «такого пользователя нет» от «имя в чёрном списке» — оба исключения одного типа.
2. В REST-слое нельзя по типу исключения вернуть правильный HTTP-статус (404 vs 409 vs 400).

**Как сделать правильно.** Заведите иерархию доменных исключений. Например:

```kotlin
// domain/exception/DomainException.kt
sealed class DomainException(message: String) : RuntimeException(message)

class UserNotFoundException(id: UUID) :
    DomainException("User with id $id not found")

class BlockedUserNameException(name: String) :
    DomainException("User name '$name' is blocked")

class FilmNotFoundException(id: UUID) :
    DomainException("Film with id $id not found")

class BlockedFilmContentException(field: String) :
    DomainException("Film $field contains blocked pattern")
```

В сервисе:

```kotlin
val user = userRepository.findById(id) ?: throw UserNotFoundException(id)
```

В REST-слое — `@RestControllerAdvice`, который мапит исключения на HTTP-ответы:

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(UserNotFoundException::class)
    fun handleNotFound(e: UserNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(e.message ?: "Not found"))

    @ExceptionHandler(BlockedUserNameException::class)
    fun handleBlocked(e: BlockedUserNameException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(e.message ?: "Blocked"))
}
```

---

### Замечание 6. Новые JDBC-репозитории сломаны — они не работают, даже если их подключить

Помимо того, что они никуда не подключены (см. замечание №4), сами по себе они **не запустятся** ни на одном запросе.

#### 6.1. `FilmRepository.findById` принимает `Int` вместо `UUID`

#### 6.2. `FilmRepository.save` — SQL не соответствует модели и параметры не сходятся

#### 6.3. `UserRepository.save` — смешаны именованные и позиционные параметры

#### 6.4. `UserRepository.findByEmail` — пропущен аргумент `email`

#### 6.5. `findTopN(sortBy)` — SQL-инъекция

**`src/main/kotlin/com/project/movienight/adapters/persistence/FilmRepository.kt:36-47`** (и аналогично в `UserRepository.kt:60`, `FilmLibraryRepository.kt:54`)

```kotlin
fun findTopN(limit: Int, sortBy: String = "name"): List<User> =
    jdbc.query("SELECT * FROM users ORDER BY $sortBy LIMIT ?", ...)
```

Параметр `sortBy` подставляется в SQL **через интерполяцию строки**. Если этот метод когда-нибудь будет вызван из контроллера с пользовательским вводом — это классическая SQL-инъекция (типа `?sortBy=name; DROP TABLE users; --`).

**Никогда не подставляйте пользовательский ввод в SQL через строковую интерполяцию.** Для значений — `?`. Для имён колонок/таблиц — белый список:

```kotlin
private val allowedSortColumns = setOf("id", "name", "email")
fun findTopN(limit: Int, sortBy: String = "name"): List<User> {
    require(sortBy in allowedSortColumns) { "Invalid sort column: $sortBy" }
    return jdbc.query("SELECT * FROM users ORDER BY $sortBy LIMIT ?", { ... }, limit)
}
```

---

### Замечание 7. Схема БД (`V1__init.sql`) не соответствует доменным моделям

**Файл:** `src/main/resources/db/migration/V1__init.sql`

Сравним, что в схеме и что в коде:

| Сущность    | В схеме (`V1__init.sql`)                                  | В домене (`domain/model/`)                                             |
|-------------|-----------------------------------------------------------|------------------------------------------------------------------------|
| `users`     | `id int4 IDENTITY, login varchar, password varchar`       | `User(id: UUID, name: String, email: String, library: FilmLibrary?)`   |
| `films`     | `id int4 IDENTITY, title, genre_id int4, issue_date date` | `Film(id: UUID, title: String, description: String)`                   |
| `favorites` | `id int4, userid int4, film_id int4, comment, is_viewed`  | `FilmLibrary(id: UUID, userId: UUID, filmId: UUID, comment, isViewed)` |

Что не так:

1. **Типы ID не совпадают.** В схеме `int4`, в коде `UUID`. Нужно одно из двух: либо в схеме `id uuid PRIMARY KEY`, либо в коде `Long`. Так как UUID везде в коде — значит в схеме переходим на `uuid`.
2. **Поля `users` совершенно другие.** В схеме `login`/`password`, в коде `name`/`email`. Это две разные сущности — кто-то рисовал схему, не глядя на модели.
3. **У `films` в схеме `genre_id` + `issue_date`**, которых нет в домене. И наоборот — в домене есть `description`, которого нет в схеме.
4. **Сломанная FK в `favorites`:** колонка называется `userid`, но FK ссылается на `user_id`:
   ```sql
   userid int4 NULL,
   ...
   CONSTRAINT favorites_users_fk FOREIGN KEY (user_id) REFERENCES public.users(id)
   ```
   Эта миграция вообще не применится — Flyway упадёт с `column "user_id" does not exist`.
5. **`DROP TABLE if exists` в Flyway-миграции** — антипаттерн. Каждая миграция должна быть **аддитивной**: создавать новое, а не уничтожать существующее. Иначе при повторном применении мигратор будет терять данные.

---

### Замечание 8. Use case'ы для `FilmLibrary` не имеют ни одной реализации

**Файлы:**

- `src/main/kotlin/com/project/movienight/application/ports/input/FilmLibraryUseCase.kt:8-49` (объявление интерфейсов)
- `src/main/kotlin/com/project/movienight/adapters/web/FilmLibraryController.kt:18-23` (инжект в контроллер)

`FilmLibraryController` инжектит четыре зависимости:

```kotlin
class FilmLibraryController(
    private val createFilmLibraryUseCase: CreateFilmLibraryUseCase,
    private val addFilmToLibraryUseCase: AddFilmToLibraryUseCase,
    private val removeFilmFromLibraryUseCase: RemoveFilmFromLibraryUseCase,
    private val getFilmLibraryUseCase: GetFilmLibraryUseCase,
)
```

**Но ни один сервис эти интерфейсы не имплементирует.**

При старте приложения **Spring не сможет создать `FilmLibraryController`** — вылетит `NoSuchBeanDefinitionException` для `CreateFilmLibraryUseCase`. Приложение просто не запустится.

---

### Замечание 9. Нет конфигурации подключения к БД

**Файл:** `src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: MovieNight

services:
  user:
    blocked-names: ...
```

В файле нет ни одного блока `spring.datasource.*`, `spring.flyway.*`.

---

### Замечание 10. `MovieNightService` — удалить

**Файл:** `src/main/kotlin/com/project/movienight/MovieNightService.kt`

Это `CommandLineRunner`, который нужен был на стадии демонстрации проекта. Теперь есть контроллеры

---

### Замечание 11. `build.gradle.kts` содержит мёртвый код и неиспользуемые зависимости
