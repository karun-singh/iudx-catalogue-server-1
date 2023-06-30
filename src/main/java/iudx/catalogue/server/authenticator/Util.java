package iudx.catalogue.server.authenticator;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import iudx.catalogue.server.authenticator.model.JwtData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Util {

  static Future<Boolean> isValidAdmin(String resourceServerUrl, JwtData jwtData, boolean isUac) {
    Promise<Boolean> promise = Promise.promise();

    if (isUac && resourceServerUrl.equalsIgnoreCase(jwtData.getClientId())) {
      promise.complete(true);
    } else if (jwtData.getRole().equalsIgnoreCase("admin")) {
      promise.complete(true);
    } else {
      promise.fail("Invalid Token: Admin token required");
    }

    return promise.future();
  }
}
