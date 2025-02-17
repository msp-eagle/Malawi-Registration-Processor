package io.mosip.registration.processor.core.abstractverticle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import brave.Tracer;
import io.mosip.registration.processor.core.token.validation.TokenValidator;
import io.mosip.registration.processor.core.tracing.VertxWrapperHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Component
public class MosipRouter {
	/** Vertx router for routes */
	private Router router;

	/** Vertx route for api */
	private Route route;

	/** Token validator class */
	@Autowired
	TokenValidator tokenValidator;

	@Autowired
	private Tracer tracer;

	/**
	 * This method sets router for API
	 * 
	 * @param router
	 */
	public void setRoute(Router router) {
		this.router = router;
	}

	/**
	 * This method returns router for API
	 * 
	 * @return
	 */
	public Router getRouter() {
		return this.router;
	}

	/**
	 * This method is used for post API call
	 * 
	 * @param url
	 * @return
	 */
	public Route post(String url) {
		this.route = this.router.post(url);
		return this.route;
	}

	/**
	 * this method is used to handle request and failure handler including
	 * validation of token
	 *
	 * @param requestHandler
	 * @param failureHandler
	 */
	public void handler(Handler<RoutingContext> requestHandler, Handler<RoutingContext> failureHandler) {
		this.route.blockingHandler(this::validateToken).blockingHandler(new VertxWrapperHandler(requestHandler){}, false)
				.failureHandler(new VertxWrapperHandler(failureHandler){});
	}

	public void nonSecureHandler(Handler<RoutingContext> requestHandler, Handler<RoutingContext> failureHandler) {
		this.route.blockingHandler(new VertxWrapperHandler(requestHandler) {}, false)
				.failureHandler(new VertxWrapperHandler(failureHandler){});
	}

	public void handler(Handler<RoutingContext> requestHandler, Handler<RoutingContext> requestHandler2,
			Handler<RoutingContext> failureHandler) {
		this.route.blockingHandler(this::validateToken)
				.blockingHandler(new VertxWrapperHandler(requestHandler){}, false)
				.blockingHandler(new VertxWrapperHandler(requestHandler2){}, false)
				.failureHandler(new VertxWrapperHandler(failureHandler){});
	}

	/**
	 * this method is used to handle request only
	 * 
	 * @param requestHandler
	 */
	public void handler(Handler<RoutingContext> requestHandler) {
		this.route.blockingHandler(requestHandler, false);
	}

	/**
	 * This method is used for get API call
	 * 
	 * @param url
	 * @return
	 */
	public Route get(String url) {
		this.route = this.router.get(url);
		return this.route;
	}

	/**
	 * This method is used for validating token
	 * 
	 * @param routingContext
	 */
	private void validateToken(RoutingContext routingContext) {
		String token = routingContext.request().getHeader("Cookie");
		String url = routingContext.normalisedPath();
		String userId = tokenValidator.validate(token, url);
		User user = new User() {

			@Override
			public JsonObject principal() {
				JsonObject principal = new JsonObject();
				principal.put("username", userId);
				return principal;
			}

			@Override
			public User isAuthorized(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
				return null;
			}

			@Override
			public User clearCache() {
				return null;
			}

			@Override
			public void setAuthProvider(AuthProvider authProvider) {
			}

		};
		routingContext.setUser(user);
		routingContext.next();
	}


}
