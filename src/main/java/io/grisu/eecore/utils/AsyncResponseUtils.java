package io.grisu.eecore.utils;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.grisu.core.exceptions.GrisuException;
import io.grisu.core.utils.ExceptionUtils;

public class AsyncResponseUtils {

    private static final Logger logger = Logger.getLogger(AsyncResponseUtils.class.getName());

    private static final Response.Status DEFAULT_NO_RESULT_STATUS = Response.Status.NOT_FOUND;

    public static <T> BiConsumer<T, Throwable> handle(AsyncResponse response) {
        return handle(response, Function.identity());
    }

    public static <T> BiConsumer<T, Throwable> handle(AsyncResponse response, Response.Status status) {
        return handle(response, Function.identity(), status);
    }

    public static <T, U> BiConsumer<T, Throwable> handle(AsyncResponse response, Function<T, U> resultTransformer) {
        return handle(response, resultTransformer, DEFAULT_NO_RESULT_STATUS);
    }

    public static <T, U> BiConsumer<T, Throwable> handle(AsyncResponse response, Function<T, U> resultTransformer, Response.Status status) {
        return handle(response, resultTransformer, () -> Response.status(status).build());
    }

    public static <T, E> BiConsumer<T, Throwable> handleWithAlternativeResult(AsyncResponse response, Supplier<E> alternativeResultSupplier) {
        return handle(response, Function.identity(), alternativeResultSupplier);
    }

    public static <T> BiConsumer<Stream<T>, Throwable> collectFrom(AsyncResponse response) {
        return handle(response, res -> res.collect(Collectors.toList()));
    }

    public static <T> BiConsumer<T, Throwable> statusOf(AsyncResponse response) {
        return statusOf(response, DEFAULT_NO_RESULT_STATUS);
    }

    public static <T> BiConsumer<T, Throwable> statusOf(AsyncResponse response, Response.Status status) {
        return handle(response, (nonNullResult) -> Response.ok().build(), status);
    }

    public static <T> BiConsumer<T, Throwable> justOk(AsyncResponse response) {
        return justOutput(response, Response.ok().build());
    }

    private static <T, U, E> BiConsumer<T, Throwable> handle(AsyncResponse response, Function<T, U> resultTransformer, Supplier<E> alternativeResultSupplier) {
        return (result, ex) -> {
            if (ex != null) {
                response.resume(handleException(ex));
                return;
            }

            if (result != null) {
                try {
                    response.resume(resultTransformer.apply(result));
                } catch (Exception e) {
                    response.resume(e);
                }
            } else {
                E alternative = alternativeResultSupplier.get();
                if (alternative instanceof Throwable) {
                    response.resume((Throwable) alternative);
                } else {
                    response.resume(alternative);
                }
            }
        };
    }

    public static <T> BiConsumer<T, Throwable> created(AsyncResponse response, Function<T, String> t) {
        return (result, ex) -> {
            if (ex != null) {
                response.resume(handleException(ex));
                return;
            }

            try {
                response.resume(Response.status(Response.Status.CREATED)
                    .location(new URI(t.apply(result)))
                    .entity(result).build());
            } catch (Exception e) {
                response.resume(handleException(e));
            }
        };
    }

    public static <T> BiConsumer<T, Throwable> justOutput(AsyncResponse response, Object output) {
        return (result, ex) -> {
            if (ex != null) {
                response.resume(handleException(ex));
                return;
            }

            response.resume(output);
        };
    }

    private static Response handleException(Throwable t) {
        final Throwable rootException = ExceptionUtils.findRootException(t);
        if (rootException instanceof GrisuException) {
            GrisuException grisuException = (GrisuException) rootException;
            return Response.status(((GrisuException) rootException).getErrorCode())
                .entity(grisuException.getErrors()).build();
        } else {
            StringWriter sw = new StringWriter();
            rootException.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            return Response.serverError().entity(rootException).build();
        }
    }

}
