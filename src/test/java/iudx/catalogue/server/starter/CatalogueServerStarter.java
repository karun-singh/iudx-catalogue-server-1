package iudx.catalogue.server.starter;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.catalogue.server.apiserver.ApiServerVerticle;
import iudx.catalogue.server.authenticator.AuthenticationVerticle;
import iudx.catalogue.server.database.DatabaseVerticle;
import iudx.catalogue.server.deploy.helper.CatalogueServerDeployer;
import iudx.catalogue.server.onboarder.OnboarderVerticle;
import iudx.catalogue.server.validator.ValidatorVerticle;

public class CatalogueServerStarter {

  private static final Logger logger = LoggerFactory.getLogger(CatalogueServerDeployer.class);
  private static Vertx vertx;
  private static ClusterManager mgr;
  private static VertxOptions options;

  /**
   * The startServer method implements the deploy helper script for deploying the the catalogue
   * server.
   */

  public Future<JsonObject> startServer() {

    Promise<JsonObject> promise = Promise.promise();

    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        vertx = res.result();

        /* Deploy the Database Service Verticle. */

        vertx.deployVerticle(new DatabaseVerticle(), databaseVerticle -> {
          if (databaseVerticle.succeeded()) {
            logger.info("The Database Service is ready");

            /* Deploy the Authentication Service Verticle. */

            vertx.deployVerticle(new AuthenticationVerticle(), authenticationVerticle -> {
              if (authenticationVerticle.succeeded()) {
                logger.info("The Authentication Service is ready");

                /* Deploy the Validator Service Verticle. */

                vertx.deployVerticle(new ValidatorVerticle(), validatorVerticle -> {
                  if (validatorVerticle.succeeded()) {
                    logger.info("The Validator Service is ready");

                    /* Deploy the Onboarder Service Verticle. */

                    vertx.deployVerticle(new OnboarderVerticle(), onboarderVerticle -> {
                      if (onboarderVerticle.succeeded()) {
                        logger.info("The Onboarder Service is ready");

                        /* Deploy the Api Server Verticle. */

                        vertx.deployVerticle(new ApiServerVerticle(), apiServerVerticle -> {
                          if (apiServerVerticle.succeeded()) {
                            logger.info("The Catalogue API Server is ready at 8443");
                            logger.info("Check /apis/ for supported APIs");
                            promise.complete(new JsonObject());
                          } else {
                            logger.info("The Catalogue API Server startup failed !");
                          }
                        });
                      } else {
                        logger.info("The Onboarder Service failed !");
                      }
                    });

                  } else {
                    logger.info("The Validator Service failed !");
                  }
                });

              } else {
                logger.info("The Authentication Service failed !");
              }
            });

          } else {
            logger.info("The Database Service failed !");
          }
        });
      } else {
        logger.info("The Vertx Cluster failed !");
      }
    });
    return promise.future();
  }

}
