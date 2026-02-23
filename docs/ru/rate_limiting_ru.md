# Ограничение пропускной способности по провайдеру (Rate Limiting)

Ограничение пропускной способности позволяет контролировать количество запросов в секунду (RPS) к каждому
LLM API провайдеру. Все модели одного провайдера используют один общий rate limiter bucket, что гарантирует
непревышение суммарного лимита запросов.

Rate limiting использует алгоритм [Bucket4j](https://github.com/bucket4j/bucket4j) (token bucket) и
**отключён по умолчанию** — активируется только при указании `rps` для провайдера.

## Конфигурация

### application.yaml

```yaml
spring:
  ai:
    ragas:
      providers:
        # Глобальные настройки rate limit (для всех провайдеров без явной конфигурации)
        rate-limit:
          default-rps: 10            # запросов в секунду (null = отключено)
          default-strategy: WAIT     # WAIT или REJECT
          default-timeout: 0         # 0 = бесконечное ожидание (без таймаута)

        openai-compatible:
          - name: openrouter
            base-url: https://openrouter.ai/api
            api-key: ${OPENROUTER_API_KEY}
            # Rate limit для конкретного провайдера (переопределяет глобальные настройки)
            rate-limit:
              rps: 5
              strategy: WAIT
              timeout: 30s
            chat-models:
              - { id: anthropic/claude-3.5-sonnet }
              - { id: google/gemini-2.5-flash }
              - { id: openai/gpt-4o-mini }

          - name: groq
            base-url: https://api.groq.com/openai
            api-key: ${GROQ_API_KEY}
            rate-limit:
              rps: 30
              strategy: REJECT
            chat-models:
              - { id: llama3-70b-8192 }

        default-provider:
          enabled: true
          rate-limit:
            rps: 10
            strategy: WAIT
          models:
            - { id: gpt-4o-mini }
```

---

## Стратегии

### WAIT

Блокирует вызывающий поток до появления свободного токена в bucket провайдера. Если настроен таймаут,
поток ожидает не дольше указанного времени, после чего выбрасывается `RateLimitExceededException`.

| Параметр |    Значение     |               Поведение                |
|----------|-----------------|----------------------------------------|
| timeout  | `0` (по умолч.) | Ожидать бесконечно до появления токена |
| timeout  | `30s`           | Ожидать до 30 секунд, затем ошибка     |
| timeout  | `500ms`         | Ожидать до 500мс, затем ошибка         |

### REJECT

Немедленно выбрасывает `RateLimitExceededException`, если токен недоступен. Параметр timeout
игнорируется для этой стратегии.

---

## Как это работает

1. **Алгоритм Token Bucket**: Каждый провайдер получает bucket с ёмкостью, равной настроенному RPS.
   Токены пополняются жадно (greedy refill) со скоростью RPS токенов в секунду.

2. **Область действия — провайдер**: Все модели одного провайдера делят один bucket. Например,
   если `openrouter` настроен с `rps: 5`, то `claude-3.5-sonnet`, `gemini-2.5-flash`
   и `gpt-4o-mini` суммарно не могут превышать 5 запросов в секунду.

3. **Graceful Failure**: Когда модель ограничена по rate limit, она возвращает `ModelResult.failure()`
   вместо необработанного исключения. Остальные модели продолжают оценку в нормальном режиме.

4. **Исключение времени ожидания**: Время ожидания rate limit **не** учитывается во времени ответа модели.
   Таймер запускается только после получения токена.

---

## Программное использование

Если вы используете `spring-ai-ragas-multi-model` без Spring Boot стартера, rate limiting можно
настроить программно:

```java
import ai.qa.solutions.execution.ratelimit.*;
import java.time.Duration;
import java.util.Map;

class Example {
    void example() {
        // Маппинг модели → провайдер
        final Map<String, String> modelToProvider = Map.of(
                "claude-3.5-sonnet", "openrouter",
                "gpt-4o-mini", "openrouter",
                "llama3-70b", "groq"
        );

        // Конфигурация rate limit для каждого провайдера
        final Map<String, RateLimitConfig> providerConfigs = Map.of(
                "openrouter", new RateLimitConfig(5, RateLimitStrategy.WAIT, Duration.ZERO),
                "groq", new RateLimitConfig(30, RateLimitStrategy.REJECT, Duration.ZERO)
        );

        // Создание реестра
        final ProviderRateLimiterRegistry registry =
                new Bucket4jProviderRateLimiterRegistry(modelToProvider, providerConfigs);

        // Передача в MultiModelExecutor
        final MultiModelExecutor executor = new MultiModelExecutor(
                chatClientStore, embeddingModelStore, metricExecutor, httpExecutor, registry);
    }
}
```

> **Важно:** При использовании библиотеки без стартера добавьте зависимость `bucket4j-core` явно:
>
> ```xml
> <dependency>
>     <groupId>com.bucket4j</groupId>
>     <artifactId>bucket4j-core</artifactId>
>     <version>8.10.1</version>
> </dependency>
> ```

---

## Справочник параметров

### Глобальные настройки

|                        Параметр                         |        Тип        |   По умолчанию   |               Описание                |
|---------------------------------------------------------|-------------------|------------------|---------------------------------------|
| `spring.ai.ragas.providers.rate-limit.default-rps`      | `Integer`         | `null` (выкл.)   | RPS по умолчанию для всех провайдеров |
| `spring.ai.ragas.providers.rate-limit.default-strategy` | `WAIT` / `REJECT` | `WAIT`           | Стратегия по умолчанию                |
| `spring.ai.ragas.providers.rate-limit.default-timeout`  | `Duration`        | `0` (бесконечно) | Таймаут для стратегии WAIT            |

### Для провайдера

|       Параметр        |        Тип        |   По умолчанию   |         Описание         |
|-----------------------|-------------------|------------------|--------------------------|
| `rate-limit.rps`      | `Integer`         | глобальное знач. | Лимит RPS для провайдера |
| `rate-limit.strategy` | `WAIT` / `REJECT` | глобальное знач. | Стратегия ограничения    |
| `rate-limit.timeout`  | `Duration`        | глобальное знач. | Таймаут для WAIT         |

Параметры доступны для `openai-compatible[*]`, `default-provider` и `external-starters.*`.

---

## Обработка ошибок

Поведение при срабатывании rate limit зависит от стратегии:

- **WAIT с таймаутом**: После истечения таймаута выбрасывается `RateLimitExceededException`, модель
  записывается как неуспешная в `ModelResult.failure()`.
- **REJECT**: Немедленно возвращает `ModelResult.failure()` для ограниченной модели.
- **Прерывание потока**: Если поток, ожидающий токен, прерван (например, при остановке приложения),
  флаг прерывания восстанавливается и выбрасывается `RateLimitExceededException`.

Во всех случаях оценка продолжается с оставшимися моделями. Ограниченная модель отображается
в списке `excludedModels` результата оценки.
