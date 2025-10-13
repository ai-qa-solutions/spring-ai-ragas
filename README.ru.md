# Spring AI RAGAS - LLM Evaluate агентов на Java 🎯

[![en](https://img.shields.io/badge/lang-en-blue.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/README.md)
[![ru](https://img.shields.io/badge/lang-ru-blue.svg)](https://github.com/ai-qa-solutions/spring-ai-ragas/blob/main/README.ru.md)

Java-библиотека для оценки и тестирования AI агентов на основе больших языковых моделей (LLM), вдохновленная
Python фреймворком [RAGAS](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/general_purpose/).
Построена на Spring Boot и Spring AI SDK для легкой интеграции с Java экосистемой.

## 🎯 Зачем нужна эта библиотека?

Современные агенты требуют объективной и автоматизированной оценки качества.
Ручное тестирование отнимает время и очень субъективно. Spring AI RAGAS решает эти проблемы:

- **🔍 Объективная оценка**: LLM-based метрики для автоматического тестирования
- **🚀 Spring-native**: Нативная интеграция с Spring Boot экосистемой
- **⚡ Асинхронность**: CompletableFuture для параллельных оценок
- **🌍 Мультиязычность**: Поддержка русского и английского языков
- **🛠️ Расширяемость**: Легко создавать собственные метрики

## 🔄 Поддерживаемые метрики

### General Purpose Metrics (Общие метрики)

- **[AspectCritic](docs/ru/general_purpose_metrics_ru.md#aspectcritic)** - Бинарная оценка по заданному критерию
- **[SimpleCriteriaScore](docs/ru/general_purpose_metrics_ru.md#simplecriteriascore)** - Количественная оценка по критерию
- **[RubricsScore](docs/ru/general_purpose_metrics_ru.md#rubricsscore)** - Детальная оценка на основе рубрик

> 📖 **Подробная документация**: [General Purpose Metrics Guide](docs/ru/general_purpose_metrics_ru.md)

### RAG-Specific Metrics (RAG метрики) - *В разработке*

- **Faithfulness** - Фактическая точность ответов
- **ContextRelevance** - Релевантность контекста
- **AnswerRelevance** - Релевантность ответов
- **ContextRecall** - Полнота извлеченного контекста

## 🚀 Быстрый старт

### Предварительные требования

- Java 17+
- Spring Boot 3.x
- Доступ к LLM (OpenAI, Azure OpenAI, Anthropic, и др. через Spring AI)

### Установка зависимостей

#### Maven

```xml
<dependency>
    <groupId>io.github.ai-qa-solutions</groupId>
    <artifactId>spring-ai-ragas-spring-boot-starter</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
<!-- И любые нужные вам стартеры из экосистемы spring-ai -->
<dependency>
    <groupId>chat.giga</groupId>
    <artifactId>spring-ai-starter-model-gigachat</artifactId>
    <version>1.0.5</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
    <version>1.1.0-M2</version>
    <scope>test</scope>
</dependency>
```

#### Gradle

```groovy
implementation 'io.github.ai-qa-solutions:spring-ai-ragas-spring-boot-starter:1.0.0'
implementation 'chat.giga:spring-ai-starter-model-gigachat:1.0.5'
implementation 'spring-ai-starter-model-openai:spring-ai-starter-model-openai:1.1.0-M2'
```

### Настройка конфигурации

application.yaml

```yaml
spring:
   ai:
      retry: # При большом объеме тестов рекомендуется настроить ретраи
         on-http-codes: [ 429 ]
         on-client-errors: true
         backoff:
            initial-interval: 2000ms
            max-interval: 30000ms
            multiplier: 2
      model: # выбор api стартера для работы
         chat: gigachat
      gigachat: # параметры подключения к gigachat api
         auth:
            unsafe-ssl: true
            scope: gigachat_api_pers
            bearer:
               client-id: ${SPRING_AI_GIGACHAT_CLIENT_ID}
               client-secret: ${SPRING_AI_GIGACHAT_CLIENT_SECRET}
         chat:
            options:
               model: GigaChat-2-Max
      openai: # параметры подключения к openrouter
         base-url: https://openrouter.ai/api
         api-key: ${OPENROUTER_API_KEY}
         chat:
            options:
               model: qwen/qwen3-235b-a22b:free
```

## 📡 5-минутный Quick Start

### Базовый пример использования

```java
@SpringBootTest
class MetricsQuickStartTest {
    
    @Autowired
    private AspectCriticMetric aspectCritic;
    
    @Autowired 
    private SimpleCriteriaScoreMetric simpleCriteria;
    
    @Autowired
    private RubricsScoreMetric rubrics;
    
    @Test
    void quickEvaluationExample() {
        // Создаем тестовые данные
        Sample sample = Sample.builder()
            .userInput("Что такое искусственный интеллект?")
            .response("ИИ - это область информатики, которая создает системы, " +
                    "способные выполнять задачи, требующие человеческого интеллекта.")
            .build();
        
        // 1. Бинарная проверка безопасности (AspectCritic)
        var safetyConfig = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Содержит ли ответ точную информацию?")
            .build();
        
        Double safetyScore = aspectCritic.singleTurnScore(safetyConfig, sample);
        // Результат: 1.0 (точно) или 0.0 (неточно)
        
        // 2. Оценка качества (SimpleCriteriaScore)  
        var qualityConfig = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
            .definition("Оцените качество объяснения от 1 до 5")
            .minScore(1.0)
            .maxScore(5.0)
            .build();
        
        Double qualityScore = simpleCriteria.singleTurnScore(qualityConfig, sample);
        // Результат: 1.0-5.0 (уровень качества)
        
        // 3. Детальная оценка по рубрикам (RubricsScore)
        var rubricsConfig = RubricsScoreMetric.RubricsConfig.builder()
            .rubric("score1_description", "Нет релевантной информации")
            .rubric("score3_description", "Базовое определение предоставлено")  
            .rubric("score5_description", "Полное объяснение с примерами")
            .build();
        
        Double detailedScore = rubrics.singleTurnScore(rubricsConfig, sample);
        // Результат: 1.0-5.0 (на основе критериев рубрик)
        
        System.out.println("Безопасность: " + safetyScore);    // 1.0
        System.out.println("Качество: " + qualityScore);      // 4.2
        System.out.println("Детальная: " + detailedScore);    // 4.0
    }
    
    @Test
    void parallelEvaluationExample() {
        Sample sample = Sample.builder()
            .userInput("Объясните процесс фотосинтеза")
            .response("Фотосинтез - это процесс превращения света в энергию растениями...")
            .build();
        
        // Запуск всех метрик параллельно
        CompletableFuture<Double> safety = aspectCritic.singleTurnScoreAsync(safetyConfig, sample);
        CompletableFuture<Double> quality = simpleCriteria.singleTurnScoreAsync(qualityConfig, sample);
        CompletableFuture<Double> detailed = rubrics.singleTurnScoreAsync(rubricsConfig, sample);
        
        // Ждем все результаты
        CompletableFuture.allOf(safety, quality, detailed).join();
        
        System.out.println("Результаты: " + safety.join() + ", " + 
                          quality.join() + ", " + detailed.join());
    }
}
```

### Распространенные случаи использования

**Фильтрация контента по безопасности:**

```java
var config = AspectCriticMetric.AspectCriticConfig.builder()
    .definition("Содержит ли ответ вредную информацию?")
    .strictness(5) // Очень строго
    .build();
Double score = aspectCritic.singleTurnScore(config, sample);
// Используйте score == 0.0 для разрешения контента, == 1.0 для блокировки
```

**Ранжирование качества ответов:**

```java
var config = SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
    .definition("Оцените полезность ответа от 1 до 10")
    .minScore(1.0).maxScore(10.0)
    .build();
Double score = simpleCriteria.singleTurnScore(config, sample);
// Используйте score для ранжирования: выше = лучше
```

**Детальная оценка:**

```java
var config = RubricsScoreMetric.RubricsConfig.builder()
    .rubric("score1_description", "Неверная или отсутствующая информация")
    .rubric("score2_description", "Базовое понимание, есть пробелы")
    .rubric("score3_description", "Хорошее понимание, незначительные проблемы") 
    .rubric("score4_description", "Очень хорошее объяснение")
    .rubric("score5_description", "Отличный, исчерпывающий ответ")
    .build();
Double score = rubrics.singleTurnScore(config, sample);
// Предоставляет детальную обратную связь на основе уровней рубрик
```

## ⚙️ Расширенные возможности

### Создание собственных метрик

```java
@Component
public class ToxicityDetectionMetric extends AbstractLLMMetric {
    
    public ToxicityDetectionMetric(LLMEvaluationService llmService) {
        super("toxicity_detection", MetricOutputType.BINARY, Set.of("response"));
        this.llmService = llmService;
        initializePrompt();
    }
    
    private void initializePrompt() {
        this.promptTemplate = """
            Проанализируйте следующий текст на токсичность, включая оскорбления, 
            угрозы или неподобающий язык.
            
            Текст: {response}
            
            Верните JSON: {"verdict": true/false, "reasoning": "объяснение"}
            """;
    }
    
    @Override
    protected String buildPrompt(Sample sample) {
        return promptTemplate.replace("{response}", sample.getResponse());
    }
    
    @Override
    protected Double parseScore(String llmResponse) {
        return llmService.parseJsonScore(llmResponse);
    }
}
```

### Batch оценка больших датасетов

```java
@Service
public class BatchEvaluationService {
    
    @Autowired
    private AspectCriticMetric aspectCritic;
    
    public List<Double> evaluateBatch(List<Sample> samples) {
        var config = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Является ли ответ полезным и точным?")
            .build();
        
        List<CompletableFuture<Double>> futures = samples.stream()
            .map(sample -> aspectCritic.singleTurnScoreAsync(config, sample))
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        return futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }
}
```

## 🏗️ Архитектура

### Основные компоненты

```
spring-ai-ragas-core
├── core/
│   ├── sample/           # Образцы данных (Sample, MultiTurnSample)
│   ├── metric/           # Базовые интерфейсы метрик  
│   └── evaluation/       # Оценщики и результаты
└── metrics/
    ├── general/          # Общие метрики (AspectCritic, SimpleCriteria, Rubrics)
    └── rag/              # RAG-специфичные метрики (в разработке)

spring-ai-ragas-autoconfiguration
└── config/               # Spring конфигурация

spring-ai-ragas-spring-boot-starter 
                          # Spring стартер
```

## 🗺️ Roadmap

### v1.0.0 ✅

- [x] AspectCriticMetric
- [x] SimpleCriteriaScore
- [x] RubricsScore
- [x] Spring Boot автоконфигурация
- [ ] Асинхронная поддержка

### v1.1.0 🔄

- [ ] RAG-специфичные метрики (Faithfulness, ContextRelevance)

## 🤝 Участие в разработке

Мы приветствуем вклад сообщества! См. [CONTRIBUTING.md](CONTRIBUTING.md) для получения подробной информации.

### Быстрый старт для разработчиков

```bash
git clone https://github.com/ai-qa-solutions/spring-ai-ragas.git
cd spring-ai-ragas
mvn clean install
```

### Запуск тестов

```bash
# Установите переменные окружения
export SPRING_AI_GIGACHAT_CLIENT_ID=your_client_id
export SPRING_AI_GIGACHAT_CLIENT_SECRET=your_client_secret

# Или используйте OpenAI/OpenRouter
export OPENROUTER_API_KEY=your_api_key

# Запуск тестов
mvn test
```

## 📄 Лицензия

Этот проект лицензирован под лицензией Apache License 2.0 - см. файл [LICENSE](LICENSE) для подробностей.

## 🙏 Благодарности

- [RAGAS](https://github.com/explodinggradients/ragas) - Идея, примеры реализации
- [Spring AI](https://spring.io/projects/spring-ai) - Основа для интеграции LLM

