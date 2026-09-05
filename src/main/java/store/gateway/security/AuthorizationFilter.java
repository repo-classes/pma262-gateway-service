package store.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class AuthorizationFilter implements GlobalFilter {

    private static Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);

    @Autowired
    private RouterValidator routerValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // get the request content
        final ServerHttpRequest request = exchange.getRequest();

        // check if the route is open
        if (!routerValidator.isSecured.test(request)) {
            return chain.filter(exchange);
        }

        // error by default
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    
}
