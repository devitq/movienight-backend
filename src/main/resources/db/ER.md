# ER

```mermaid
erDiagram
    users {
        UUID id PK
        VARCHAR name
        VARCHAR email
    }

    films {
        UUID id PK
        VARCHAR title
        TEXT description
    }

    favorites {
        UUID id PK
        UUID user_id FK
        UUID film_id FK
        VARCHAR comment
        BOOLEAN is_viewed
    }

    users ||--o{ favorites : has
    films ||--o{ favorites : linked
```
