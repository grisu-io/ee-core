package io.grisu.eecore.utils;

import javax.ws.rs.core.Response;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface AsyncResponseIFace {

    <T> BiConsumer<T, Throwable> handle(javax.ws.rs.container.AsyncResponse response);

    <T> BiConsumer<T, Throwable> handle(javax.ws.rs.container.AsyncResponse response, Response.Status status);

    <T, U> BiConsumer<T, Throwable> handle(javax.ws.rs.container.AsyncResponse response, Function<T, U> resultTransformer);

    <T, U> BiConsumer<T, Throwable> handle(javax.ws.rs.container.AsyncResponse response, Function<T, U> resultTransformer, Response.Status status);

    <T, E> BiConsumer<T, Throwable> handleWithAlternativeResult(javax.ws.rs.container.AsyncResponse response, Supplier<E> alternativeResultSupplier);

    <T> BiConsumer<Stream<T>, Throwable> collectFrom(javax.ws.rs.container.AsyncResponse response);

    <T> BiConsumer<T, Throwable> statusOf(javax.ws.rs.container.AsyncResponse response);

    <T> BiConsumer<T, Throwable> statusOf(javax.ws.rs.container.AsyncResponse response, Response.Status status);

    <T> BiConsumer<T, Throwable> justOk(javax.ws.rs.container.AsyncResponse response);

    <T, U, E> BiConsumer<T, Throwable> handle(javax.ws.rs.container.AsyncResponse response, Function<T, U> resultTransformer, Supplier<E> alternativeResultSupplier);

    <T> BiConsumer<T, Throwable> created(javax.ws.rs.container.AsyncResponse response, Function<T, String> t);

    <T> BiConsumer<T, Throwable> justOutput(javax.ws.rs.container.AsyncResponse response, Object output);

}