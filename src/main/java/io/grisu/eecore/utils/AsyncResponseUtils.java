package io.grisu.eecore.utils;

import io.grisu.core.exceptions.GrisuException;
import io.grisu.core.utils.ExceptionUtils;
import io.grisu.core.utils.MapBuilder;

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

public class AsyncResponseUtils implements AsyncResponseIFace {

    private final Logger logger = Logger.getLogger(io.grisu.eecore.utils.AsyncResponseUtils.class.getName());

    private final Response.Status DEFAULT_NO_RESULT_STATUS = Response.Status.NOT_FOUND;

    @Override
    public <T> BiConsumer<T, Throwable> handle(AsyncResponse response) {
        return handle(response, Function.identity());
    }

    @Override
    public <T> BiConsumer<T, Throwable> handle(AsyncResponse response, Response.Status status) {
        return handle(response, Function.identity(), status);
    }

    @Override
    public <T, U> BiConsumer<T, Throwable> handle(AsyncResponse response, Function<T, U> resultTransformer) {
        return handle(response, resultTransformer, DEFAULT_NO_RESULT_STATUS);
    }

    @Override
    public <T, U> BiConsumer<T, Throwable> handle(AsyncResponse response, Function<T, U> resultTransformer, Response.Status status) {
        return handle(response, resultTransformer, () -> Response.status(status).build());
    }

    @Override
    public <T, E> BiConsumer<T, Throwable> handleWithAlternativeResult(AsyncResponse response, Supplier<E> alternativeResultSupplier) {
        return handle(response, Function.identity(), alternativeResultSupplier);
    }

    @Override
    public <T> BiConsumer<Stream<T>, Throwable> collectFrom(AsyncResponse response) {
        return handle(response, res -> res.collect(Collectors.toList()));
    }

    @Override
    public <T> BiConsumer<T, Throwable> statusOf(AsyncResponse response) {
        return statusOf(response, DEFAULT_NO_RESULT_STATUS);
    }

    @Override
    public <T> BiConsumer<T, Throwable> statusOf(AsyncResponse response, Response.Status status) {
        return handle(response, (nonNullResult) -> Response.ok().build(), status);
    }

    @Override
    public <T> BiConsumer<T, Throwable> justOk(AsyncResponse response) {
        return justOutput(response, Response.ok().build());
    }

    @Override
    public <T, U, E> BiConsumer<T, Throwable> handle(AsyncResponse response, Function<T, U> resultTransformer, Supplier<E> alternativeResultSupplier) {
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

    @Override
    public <T> BiConsumer<T, Throwable> created(AsyncResponse response, Function<T, String> t) {
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

    @Override
    public <T> BiConsumer<T, Throwable> justOutput(AsyncResponse response, Object output) {
        return (result, ex) -> {
            if (ex != null) {
                response.resume(handleException(ex));
                return;
            }

            response.resume(output);
        };
    }

    public Response handleException(Throwable t) {
        final Throwable rootException = ExceptionUtils.findRootException(t);
        if (rootException instanceof GrisuException) {
            GrisuException grisuException = (GrisuException) rootException;
            return Response
                .status(grisuException.getErrorCode())
                .entity(grisuException.getErrors())
                .build();
        } else {
            StringWriter sw = new StringWriter();
            rootException.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            return Response.serverError().entity(MapBuilder
                .instance().add("error", "Aliens have kidnapped the actual cause, I'm sorry!").build()).build();
        }
    }

}