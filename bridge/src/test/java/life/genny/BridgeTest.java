package life.genny;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class BridgeTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/api/events/init?url=http://localhost:8080")
          .then()
             .statusCode(200);
    }

}
