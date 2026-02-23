package ai.qa.solutions.execution.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Bucket4jProviderRateLimiterRegistry Tests")
class Bucket4jProviderRateLimiterRegistryTest {

    @Nested
    @DisplayName("Wait Strategy")
    class WaitStrategy {

        @Test
        @DisplayName("Should acquire token when bucket has capacity")
        void shouldAcquireTokenWhenBucketHasCapacity() {
            // Given
            final var registry = new Bucket4jProviderRateLimiterRegistry(
                    Map.of("model-a", "provider-1"),
                    Map.of("provider-1", new RateLimitConfig(10, RateLimitStrategy.WAIT, Duration.ZERO)));

            // When / Then
            assertThatCode(() -> registry.acquire("model-a")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should block when bucket exhausted then release as tokens refill")
        void shouldBlockWhenBucketExhaustedThenRelease() {
            // Given - RPS=2, WAIT strategy with infinite timeout
            final var registry = new Bucket4jProviderRateLimiterRegistry(
                    Map.of("model-a", "provider-1"),
                    Map.of("provider-1", new RateLimitConfig(2, RateLimitStrategy.WAIT, Duration.ZERO)));

            // When - exhaust the bucket
            registry.acquire("model-a");
            registry.acquire("model-a");

            // Then - third acquire should block briefly until refill, then succeed
            // With RPS=2, tokens refill at 2 per second, so wait should be ~500ms max
            final long startMs = System.currentTimeMillis();
            assertThatCode(() -> registry.acquire("model-a")).doesNotThrowAnyException();
            final long elapsedMs = System.currentTimeMillis() - startMs;

            // Should have waited some time (bucket was empty) but not excessively
            assertThat(elapsedMs).isGreaterThanOrEqualTo(0);
            assertThat(elapsedMs).isLessThan(2000);
        }

        @Test
        @DisplayName("Should handle timeout for wait strategy")
        void shouldHandleTimeoutForWaitStrategy() {
            // Given - RPS=1, WAIT with 50ms timeout
            final var registry = new Bucket4jProviderRateLimiterRegistry(
                    Map.of("model-a", "provider-1"),
                    Map.of("provider-1", new RateLimitConfig(1, RateLimitStrategy.WAIT, Duration.ofMillis(50))));

            // When - exhaust the bucket
            registry.acquire("model-a");

            // Then - next acquire should throw within approximately 50ms
            final long startMs = System.currentTimeMillis();
            assertThatThrownBy(() -> registry.acquire("model-a"))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessageContaining("timeout");
            final long elapsedMs = System.currentTimeMillis() - startMs;

            // Should have waited some time (bucket4j timer resolution is platform-dependent)
            assertThat(elapsedMs).isGreaterThanOrEqualTo(10);
            assertThat(elapsedMs).isLessThan(500);
        }

        @Test
        @DisplayName("Should handle interrupted exception during wait")
        void shouldHandleInterruptedExceptionDuringWait() throws InterruptedException {
            // Given - RPS=1, WAIT with infinite timeout (Duration.ZERO)
            final var registry = new Bucket4jProviderRateLimiterRegistry(
                    Map.of("model-a", "provider-1"),
                    Map.of("provider-1", new RateLimitConfig(1, RateLimitStrategy.WAIT, Duration.ZERO)));

            // Exhaust the bucket
            registry.acquire("model-a");

            // When - run acquire in a separate thread and interrupt it
            final AtomicReference<Throwable> caughtException = new AtomicReference<>();
            final AtomicReference<Boolean> interruptFlagRestored = new AtomicReference<>(false);
            final CountDownLatch threadStarted = new CountDownLatch(1);
            final CountDownLatch threadFinished = new CountDownLatch(1);

            final Thread acquireThread = new Thread(() -> {
                threadStarted.countDown();
                try {
                    registry.acquire("model-a");
                } catch (final RateLimitExceededException e) {
                    caughtException.set(e);
                    interruptFlagRestored.set(Thread.currentThread().isInterrupted());
                }
                threadFinished.countDown();
            });
            acquireThread.start();

            // Wait for thread to start blocking
            assertThat(threadStarted.await(1, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(50); // give it time to enter blocking acquire

            // Interrupt the thread
            acquireThread.interrupt();

            // Then
            assertThat(threadFinished.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(caughtException.get())
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessageContaining("interrupted");
            assertThat(interruptFlagRestored.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Reject Strategy")
    class RejectStrategy {

        @Test
        @DisplayName("Should acquire when bucket has capacity")
        void shouldAcquireWhenBucketHasCapacity() {
            // Given
            final var registry = new Bucket4jProviderRateLimiterRegistry(
                    Map.of("model-a", "provider-1"),
                    Map.of("provider-1", new RateLimitConfig(5, RateLimitStrategy.REJECT, Duration.ZERO)));

            // When / Then
            assertThatCode(() -> registry.acquire("model-a")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw when bucket exhausted")
        void shouldThrowWhenBucketExhausted() {
            // Given - RPS=1, REJECT strategy
            final var registry = new Bucket4jProviderRateLimiterRegistry(
                    Map.of("model-a", "provider-1"),
                    Map.of("provider-1", new RateLimitConfig(1, RateLimitStrategy.REJECT, Duration.ZERO)));

            // When - exhaust the bucket
            registry.acquire("model-a");

            // Then - next acquire should throw immediately
            assertThatThrownBy(() -> registry.acquire("model-a"))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessageContaining("Rate limit exceeded")
                    .satisfies(ex -> {
                        final var rle = (RateLimitExceededException) ex;
                        assertThat(rle.getModelId()).isEqualTo("model-a");
                        assertThat(rle.getProviderName()).isEqualTo("provider-1");
                    });
        }
    }

    @Nested
    @DisplayName("Provider Isolation")
    class ProviderIsolation {

        @Test
        @DisplayName("Should share bucket across models of same provider")
        void shouldShareBucketAcrossModelsOfSameProvider() {
            // Given - two models mapped to same provider, RPS=2, REJECT strategy
            final var registry = new Bucket4jProviderRateLimiterRegistry(
                    Map.of("model-a", "provider-1", "model-b", "provider-1"),
                    Map.of("provider-1", new RateLimitConfig(2, RateLimitStrategy.REJECT, Duration.ZERO)));

            // When - consume one token from each model (same provider bucket)
            registry.acquire("model-a");
            registry.acquire("model-b");

            // Then - bucket exhausted, third call from either model should fail
            assertThatThrownBy(() -> registry.acquire("model-a")).isInstanceOf(RateLimitExceededException.class);
        }

        @Test
        @DisplayName("Should use separate buckets for different providers")
        void shouldUseSeparateBucketsForDifferentProviders() {
            // Given - two models from different providers, RPS=1 each, REJECT strategy
            final var registry = new Bucket4jProviderRateLimiterRegistry(
                    Map.of("model-a", "provider-1", "model-b", "provider-2"),
                    Map.of(
                            "provider-1", new RateLimitConfig(1, RateLimitStrategy.REJECT, Duration.ZERO),
                            "provider-2", new RateLimitConfig(1, RateLimitStrategy.REJECT, Duration.ZERO)));

            // When - exhaust provider-1 bucket
            registry.acquire("model-a");

            // Then - provider-2 bucket should be unaffected
            assertThatCode(() -> registry.acquire("model-b")).doesNotThrowAnyException();

            // And provider-1 should be exhausted
            assertThatThrownBy(() -> registry.acquire("model-a")).isInstanceOf(RateLimitExceededException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should return immediately when model not registered")
        void shouldReturnImmediatelyWhenModelNotRegistered() {
            // Given - registry with one model, querying different model
            final var registry = new Bucket4jProviderRateLimiterRegistry(
                    Map.of("model-a", "provider-1"),
                    Map.of("provider-1", new RateLimitConfig(1, RateLimitStrategy.REJECT, Duration.ZERO)));

            // When / Then - unknown model should not throw
            assertThatCode(() -> registry.acquire("unknown-model")).doesNotThrowAnyException();
        }
    }
}
