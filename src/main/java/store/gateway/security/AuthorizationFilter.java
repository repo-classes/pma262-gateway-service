package store.gateway.security;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class AuthorizationFilter implements GlobalFilter {

    private static Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);

    public static String AUTH_COOKIE_TOKEN = "__store_jwt_token";
    public static String AUTH_SERVICE_TOKEN_SOLVE = "http://auth:8080/auth/solve";

    @Autowired
    private RouterValidator routerValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // get the request content
        final ServerHttpRequest request = exchange.getRequest();

        // check if the route is open
        if (!routerValidator.isSecured.test(request)) {
            logger.debug("this route is open");
            return chain.filter(exchange);
        }

        // route is not open
        // check if the cookie exists
        if (request.getCookies().containsKey(AUTH_COOKIE_TOKEN)) {
            logger.debug("I got the cookie");
            // get cookie valuw that is the jwt
            String token = request.getCookies().getFirst(AUTH_COOKIE_TOKEN).getValue();
            if (null != token && token.length() > 0) {
                logger.debug("Cookie is not empty");
                return requestAuthTokenSolve(exchange, chain, token.trim());
            }
        }

        // error by default
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

    private Mono<Void> requestAuthTokenSolve(
        ServerWebExchange exchange,
        GatewayFilterChain chain,
        final String jwt) {

        return WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
            .post()
            .uri(AUTH_SERVICE_TOKEN_SOLVE)
            .bodyValue(Map.of("token", jwt))
            .retrieve()
            // auth rejected the token (expired, tampered...): 401, not a 500
            .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token")
            ))
            .toEntity(Map.class)
            .flatMap(response -> {
                if (response != null && response.hasBody() && response.getBody() != null) {
                    final Map<String, String> map = response.getBody();
                    logger.debug(map.toString());
                    String idAccount = map.get("idAccount");
                    logger.debug("header before update: " + exchange.getRequest().getHeaders().toString());
                    ServerWebExchange authorized = updateRequest(exchange, idAccount, jwt);
                    logger.debug("header after update: " + authorized.getRequest().getHeaders().toString());
                    return chain.filter(authorized);
                } else {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
                }
            });
    }

    private ServerWebExchange updateRequest(ServerWebExchange exchange, String idAccount, String jwt) {
        return exchange.mutate()
            .request(
                exchange.getRequest()
                    .mutate()
                    .header("id-account", idAccount)
                    // .header("Authorization", "Bearer " + jwt)
                    .build()
            ).build();
    }

}
