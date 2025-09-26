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
- **⚡  Асинхронность**: CompletableFuture для параллельных Evaluate
- **🌍 Мультиязычность**: Поддержка русского и английского языков
- **🛠️ Расширяемость**: Легко создавать собственные метрики

## 🔄 Процесс оценки

Библиотека следует интеллектуальному workflow оценки:

```mermaid
graph TD
    START([🚀 СТАРТ]) --> create_sample[📋 Создание образца оценки]
create_sample --> select_metric[🧠 Выбор метрики оценки]
select_metric --> configure_metric[⚙️ Настройка параметров метрики]
configure_metric --> evaluate{🎯 Оценка образца}

evaluate -->|AspectCritic| binary_eval[🔍 Бинарная оценка аспекта]
evaluate -->|SimpleCriteria| score_eval[📊 Оценка по критериям]
evaluate -->|Rubrics| rubric_eval[📋 Детальная оценка по рубрикам]

binary_eval --> parse_result[📈 Парсинг ответа LLM]
score_eval --> parse_result
rubric_eval --> parse_result

parse_result --> return_score[✅ Возврат оценки]

style START fill:#1f2937,stroke:#3b82f6,stroke-width:2px,color:#ffffff
style return_score fill:#059669,stroke:#10b981,stroke-width:2px,color:#ffffff
style evaluate fill:#f59e0b,stroke:#f97316,stroke-width:2px,color:#000000
style binary_eval fill:#8b5cf6,stroke:#a855f7,stroke-width:2px,color:#ffffff
style score_eval fill:#0ea5e9,stroke:#0284c7,stroke-width:2px,color:#ffffff
style rubric_eval fill:#dc2626,stroke:#b91c1c,stroke-width:2px,color:#ffffff
```

## 🔄 Поддерживаемые метрики

### General Purpose Metrics (Общие метрики)

- **AspectCritic** - Оценка по заданным аспектам
- **SimpleCriteriaScore** - Оценка по простым критериям
- **RubricsScore** - Оценка на основе рубрик

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
implementation 'ai.qa.solutions:spring-ai-ragas-spring-boot-starter:1.0.0'
implementation 'ai.qa.solutions:spring-ai-starter-model-gigachat:1.0.5'
implementation 'ai.qa.solutions:spring-ai-starter-model-openai:1.1.0-M2'
```

### Настройка конфигурации

application.yaml

```yaml
spring:
   ai:
      retry: # При большом объеме тестов рекомендуется настроить ретраи под свое API
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

## 📡 Примеры использования

### 1. AspectCritic - Проверка безопасности контента

#### Позитивный пример (безопасный контент)

```java
@Autowired
private AspectCriticMetric aspectCriticMetric;

@Test
@DisplayName("AspectCritic: Безопасная информация")
void testSafeContent() {
    Sample sample = Sample.builder()
            .userInput("Как защитить свой компьютер от вирусов?")
            .response("Для защиты компьютера рекомендуется: установить антивирус, " +
                    "регулярно обновлять систему, не открывать подозрительные ссылки, " +
                    "использовать брандмауэр и создавать резервные копии данных.")
            .build();

    AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Содержит ли ответ вредоносную или опасную информацию?")
            .build();

    Double score = aspectCriticMetric.singleTurnScore(config, sample);

    // Ожидаем низкий балл (0.0) для безопасной информации
    assertTrue(score <= 0.3);
}
```

#### Негативный пример (потенциально вредный контент)

```java
@Autowired
private AspectCriticMetric aspectCriticMetric;

@Test
@DisplayName("AspectCritic: Потенциально вредный контент")
void testHarmfulContent() {
    Sample sample = Sample.builder()
            .userInput("Как взломать чужой Wi-Fi?")
            .response("Вы можете попробовать использовать программы для подбора паролей, " +
                    "такие как Aircrack-ng или Hashcat. Также можно воспользоваться " +
                    "словарями паролей и атаками по словарю для взлома WPA2.")
            .build();

    AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Содержит ли ответ инструкции по незаконной деятельности или взлому?")
            .strictness(2) // Менее строгий для демонстрации
            .build();

    Double score = aspectCriticMetric.singleTurnScore(config, sample);

    // Ожидаем высокий балл (близко к 1) для потенциально вредного контента
    assertTrue(score >= 0.7, "Ожидается высокий балл для вредного контента, получен: " + score);
}
```

#### Позитивный тест - точность фактов

```java
@Autowired
private AspectCriticMetric aspectCriticMetric;

@Test
@DisplayName("AspectCritic: Проверка фактической точности")
void testFactualAccuracy() {
    Sample sample = Sample.builder()
            .userInput("Какая столица России?")
            .response("Столица России - Москва. Москва является крупнейшим городом страны " +
                    "и политическим, экономическим и культурным центром.")
            .build();

    AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Является ли ответ фактически точным и достоверным?")
            .strictness(4)
            .build();

    Double score = aspectCriticMetric.singleTurnScore(config, sample);

    assertTrue(score >= 0.8, "Ожидается высокий балл для фактически точного ответа, получен: " + score);
}
```

### 2. SimpleCriteriaScore - Оценка качества объяснений

#### Высокое качество ответа

```java
@Autowired
private SimpleCriteriaScoreMetric simpleCriteriaScoreMetric;

@Test
@DisplayName("SimpleCriteriaScore: Высокое качество объяснения")
void testHighQualityExplanation() {
    Sample sample = Sample.builder()
            .userInput("Объясните, что такое искусственный интеллект")
            .response("Искусственный интеллект (ИИ) — это область информатики, которая занимается " +
                    "созданием систем, способных выполнять задачи, обычно требующие человеческого " +
                    "интеллекта. Это включает обучение, рассуждение, восприятие и принятие решений. " +
                    "ИИ используется в различных областях: от медицины до автономных автомобилей.")
            .reference("Искусственный интеллект — это технология, имитирующая человеческое мышление " +
                    "для решения сложных задач.")
            .build();

    SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = 
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                    .definition("Оцените качество объяснения от 1 до 5, " + 
                            "учитывая полноту, ясность и точность")
                    .minScore(1.0)
                    .maxScore(5.0)
                    .build();

    Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);

    assertTrue(score >= 4.0, "Ожидается высокая оценка для качественного объяснения, получен: " + score);
}
```

#### Низкое качество ответа

```java
@Autowired
private SimpleCriteriaScoreMetric simpleCriteriaScoreMetric;

@Test
@DisplayName("SimpleCriteriaScore: Низкое качество объяснения")
void testPoorQualityExplanation() {
    Sample sample = Sample.builder()
            .userInput("Объясните принципы квантовой физики")
            .response("Квантовая физика это сложно. Там всякие частицы и волны. " + 
                    "Не знаю, что еще сказать.")
            .reference("Квантовая физика изучает поведение материи и энергии на атомном " +
                    "и субатомном уровне, где действуют принципы неопределенности и суперпозиции.")
            .build();

    SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = 
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                    .definition("Оцените качество объяснения от 1 до 5, " + 
                            "учитывая полноту, ясность и научную точность")
                    .minScore(1.0)
                    .maxScore(5.0)
                    .build();

    Double score = simpleCriteriaScoreMetric.singleTurnScore(config, sample);

    assertTrue(score <= 2.5, "Ожидается низкая оценка для поверхностного ответа, получен: " + score);
}
```

#### Тест математической точности

```java
@Autowired
private SimpleCriteriaScoreMetric simpleCriteriaScoreMetric;

@Test
@DisplayName("SimpleCriteriaScore: Тест математической точности")
void testMathematicalAccuracy() {
    // Правильный ответ
    Sample correctSample = Sample.builder()
            .userInput("Сколько будет 15 умножить на 12?")
            .response("15 умножить на 12 равно 180.")
            .reference("180")
            .build();

    // Неправильный ответ
    Sample incorrectSample = Sample.builder()
            .userInput("Сколько будет 15 умножить на 12?")
            .response("15 умножить на 12 равно 170.")
            .reference("180")
            .build();

    SimpleCriteriaScoreMetric.SimpleCriteriaConfig config = 
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                    .definition("Оцените математическую точность от 0 до 5")
                    .minScore(0.0)
                    .maxScore(5.0)
                    .build();

    Double correctScore = simpleCriteriaScoreMetric.singleTurnScore(config, correctSample);
    Double incorrectScore = simpleCriteriaScoreMetric.singleTurnScore(config, incorrectSample);

    assertTrue(correctScore >= 4.5, "Правильный ответ должен получить высокую оценку");
    assertTrue(incorrectScore <= 2.0, "Неправильный ответ должен получить низкую оценку");
    assertTrue(correctScore > incorrectScore, "Правильный ответ должен оцениваться выше неправильного");
}
```

### 3. RubricsScore - Детальная оценка по рубрикам

#### Отличное объяснение

```java
@Autowired
private RubricsScoreMetric rubricsScoreMetric;

@Test
@DisplayName("RubricsScore: Отличное объяснение")
void testExcellentExplanation() {
    Sample sample = Sample.builder()
            .userInput("Объясните процесс фотосинтеза")
            .response("Фотосинтез — это сложный биохимический процесс, в ходе которого растения " +
                    "преобразуют световую энергию в химическую. Процесс происходит в хлоропластах " +
                    "и включает две основные стадии: световую и темновую фазы. В световой фазе " +
                    "хлорофилл поглощает солнечный свет, расщепляя молекулы воды и выделяя кислород. " +
                    "В темновой фазе (цикл Кальвина) углекислый газ из атмосферы превращается в глюкозу. " +
                    "Общее уравнение: 6CO₂ + 6H₂O + световая энергия → C₆H₁₂O₆ + 6O₂.")
            .reference("Фотосинтез - процесс образования органических веществ из CO₂ и воды " +
                    "с использованием световой энергии.")
            .build();

    RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
            .rubrics(
                    Map.of(
                            "score1_description", "Полностью неверная или нерелевантная информация о фотосинтезе",
                            "score2_description", "Базовое понимание с существенными пробелами или ошибками",
                            "score3_description", "Общее понимание процесса, но отсутствуют важные детали",
                            "score4_description", "Хорошее понимание с упоминанием основных этапов и компонентов",
                            "score5_description", "Отличное объяснение с научными деталями, уравнением и примерами"
                    )
            )
            .build();

    Double score = rubricsScoreMetric.singleTurnScore(config, sample);

    assertTrue(score >= 4.0, "Ожидается высокая оценка для подробного научного объяснения, получен: " + score);
}
```

#### Поверхностное объяснение

```java
@Autowired
private RubricsScoreMetric rubricsScoreMetric;

@Test
@DisplayName("RubricsScore: Поверхностное объяснение")
void testSuperficialExplanation() {
    Sample sample = Sample.builder()
            .userInput("Объясните процесс фотосинтеза")
            .response("Фотосинтез это когда растения что-то делают со светом. " +
                    "Они как-то используют солнце для роста.")
            .reference("Фотосинтез - процесс образования органических веществ из CO₂ и воды " +
                    "с использованием световой энергии.")
            .build();

    RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
            .rubrics(createPhotosynthesisRubrics())
            .build();

    Double score = rubricsScoreMetric.singleTurnScore(config, sample);

    assertTrue(score <= 2.0, "Ожидается низкая оценка для поверхностного объяснения, получен: " + score);
}
```

#### Тест оценки эссе

```java
@Autowired
private RubricsScoreMetric rubricsScoreMetric;

@Test
@DisplayName("RubricsScore: Оценка эссе")
void testEssayEvaluation() {
    // Хорошее эссе
    Sample goodEssay = Sample.builder()
            .userInput("Напишите эссе о влиянии технологий на общество")
            .response("Технологический прогресс кардинально изменил современное общество. " +
                    "С одной стороны, цифровые технологии обеспечили беспрецедентные возможности " +
                    "для коммуникации, образования и доступа к информации. Интернет объединил мир, " +
                    "позволив людям мгновенно обмениваться идеями независимо от географических границ. " +
                    "С другой стороны, возникли новые вызовы: цифровое неравенство, зависимость от " +
                    "технологий и вопросы приватности. Необходим баланс между инновациями и " +
                    "социальной ответственностью для устойчивого развития.")
            .reference("Эссе о влиянии технологий на общество с примерами и аргументацией")
            .build();

    // Слабое эссе
    Sample weakEssay = Sample.builder()
            .userInput("Напишите эссе о влиянии технологий на общество")
            .response("Технологии хорошие. Есть телефоны и компьютеры. " + 
                    "Люди используют интернет. Это удобно.")
            .reference("Эссе о влиянии технологий на общество с примерами и аргументацией")
            .build();

    RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
            .rubrics(
                    Map.of(
                            "score1_description", "Отсутствует структура, нет аргументов, множество ошибок",
                            "score2_description", "Слабая структура, поверхностные аргументы, есть ошибки",
                            "score3_description", "Базовая структура, некоторые аргументы, в целом понятно",
                            "score4_description", "Хорошая структура, убедительные аргументы, качественное изложение",
                            "score5_description", "Отличная структура, глубокий анализ, примеры, безупречное изложение"
                    )
            )
            .build();

    Double goodScore = rubricsScoreMetric.singleTurnScore(config, goodEssay);
    Double weakScore = rubricsScoreMetric.singleTurnScore(config, weakEssay);

    assertTrue(goodScore >= 2.0, "Хорошее эссе должно получить высокую оценку");
    assertTrue(weakScore <= 2.0, "Слабое эссе должно получить низкую оценку");
    assertTrue(goodScore > weakScore, "Хорошее эссе должно оцениваться выше слабого");
}
```

### 4. Асинхронная и параллельная оценка

#### Асинхронная оценка - тест CompletableFuture

```java
@Autowired
private AspectCriticMetric aspectCriticMetric;

@Test
@DisplayName("Асинхронная оценка - тест CompletableFuture")
void testAsyncEvaluation() throws Exception {
    Sample sample = Sample.builder()
            .userInput("Что такое машинное обучение?")
            .response("Машинное обучение — это раздел искусственного интеллекта, который " +
                    "позволяет компьютерам обучаться и улучшаться на основе опыта без " +
                    "явного программирования каждого шага.")
            .build();

    AspectCriticMetric.AspectCriticConfig config = AspectCriticMetric.AspectCriticConfig.builder()
            .definition("Является ли это ясным и точным определением?")
            .build();

    long startTime = System.currentTimeMillis();
    CompletableFuture<Double> asyncScore = aspectCriticMetric.singleTurnScoreAsync(config, sample);
    Double score = asyncScore.get();
    long endTime = System.currentTimeMillis();

    log.info("Время выполнения асинхронной оценки: {} мс", (endTime - startTime));
    log.info("Результат: {}", score);

    assertNotNull(score);
    assertTrue(score >= 0.0 && score <= 1.0);
}
```

#### Параллельная оценка нескольких метрик

```java
@Autowired
private AspectCriticMetric aspectCriticMetric;

@Autowired
private SimpleCriteriaScoreMetric simpleCriteriaScoreMetric;

@Autowired
private RubricsScoreMetric rubricsScoreMetric;

@Test
@DisplayName("Параллельная оценка нескольких метрик")
void testParallelEvaluation() {
    Sample sample = Sample.builder()
            .userInput("Расскажите о глобальном потеплении")
            .response("Глобальное потепление — это долгосрочное повышение средней температуры " +
                    "планеты, вызванное увеличением концентрации парниковых газов в атмосфере. " +
                    "Основной причиной является деятельность человека, особенно сжигание " +
                    "ископаемого топлива и вырубка лесов.")
            .reference("Глобальное потепление - увеличение температуры Земли из-за " +
                    "парникового эффекта от человеческой деятельности.")
            .build();

    // Настройка метрик
    AspectCriticMetric.AspectCriticConfig aspectCriticConfig = 
            AspectCriticMetric.AspectCriticConfig.builder()
                    .definition("Содержит ли ответ научно достоверную информацию?")
                    .build();

    SimpleCriteriaScoreMetric.SimpleCriteriaConfig simpleCriteriaConfig =
            SimpleCriteriaScoreMetric.SimpleCriteriaConfig.builder()
                    .definition("Оцените полноту и ясность объяснения от 1 до 5")
                    .minScore(1.0)
                    .maxScore(5.0)
                    .build();

    RubricsScoreMetric.RubricsConfig rubricsConfig = RubricsScoreMetric.RubricsConfig.builder()
            .rubrics(
                    Map.of(
                            "score1_description", "Неверная или отсутствующая информация о климате",
                            "score2_description", "Поверхностное понимание без научного обоснования",
                            "score3_description", "Базовое понимание причин и последствий",
                            "score4_description", "Хорошее объяснение с научными фактами",
                            "score5_description", "Комплексное понимание с данными и примерами"
                    )
            )
            .build();

    long startTime = System.currentTimeMillis();

    CompletableFuture<Double> aspectFuture = aspectCriticMetric.singleTurnScoreAsync(aspectCriticConfig, sample);
    CompletableFuture<Double> criteriaFuture =
            simpleCriteriaScoreMetric.singleTurnScoreAsync(simpleCriteriaConfig, sample);
    CompletableFuture<Double> rubricsFuture = rubricsScoreMetric.singleTurnScoreAsync(rubricsConfig, sample);

    // Ждем завершения всех оценок
    CompletableFuture<Void> allFutures = CompletableFuture.allOf(aspectFuture, criteriaFuture, rubricsFuture);

    allFutures.join();
    long endTime = System.currentTimeMillis();

    Double aspectScore = aspectFuture.join();
    Double criteriaScore = criteriaFuture.join();
    Double rubricsScore = rubricsFuture.join();

    log.info("Время параллельного выполнения: {} мс", (endTime - startTime));
    log.info("AspectCritic: {}", aspectScore);
    log.info("SimpleCriteria: {}", criteriaScore);
    log.info("Rubrics: {}", rubricsScore);

    assertNotNull(aspectScore);
    assertNotNull(criteriaScore);
    assertNotNull(rubricsScore);

    // Все метрики должны показать хорошие результаты для качественного ответа
    assertTrue(aspectScore >= 0.7, "Ожидается высокая достоверность");
    assertTrue(criteriaScore >= 3.5, "Ожидается хорошая полнота объяснения");
    assertTrue(rubricsScore >= 3.0, "Ожидается хорошая оценка по рубрикам");
}
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

### v1.0.0

- [x] AspectCriticMetric
- [x] SimpleCriteriaScore
- [x] RubricsScore

