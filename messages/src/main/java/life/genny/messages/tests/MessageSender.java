//package life.genny.messages.tests;
//
//import io.quarkus.test.junit.QuarkusTest;
//import life.genny.messages.managers.QEmailMessageManager;
//import life.genny.messages.managers.QMessageFactory;
//import life.genny.qwandaq.entity.BaseEntity;
//import life.genny.qwandaq.message.QBaseMSGMessageType;
//import life.genny.qwandaq.message.QCommsMessage;
//import life.genny.qwandaq.utils.BaseEntityUtils;
//import org.jose4j.json.internal.json_simple.parser.ParseException;
//import org.junit.jupiter.api.Test;
//
//import javax.json.bind.Jsonb;
//import javax.json.bind.JsonbBuilder;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//
////@QuarkusTest
//public class MessageSender {
//    Path fileName = Path.of("/Users/Rahul/projects/genny/gennyq/messages/src/main/java/life/genny/messages/tests/searchJson.json");
//    String searchJson = Files.readString(fileName);
//
//    Jsonb jsonb = JsonbBuilder.create();
//
//    public MessageSender() throws IOException, ParseException {
//    }
//
//    @Test
//    public void simpleTest() {
//        BaseEntityUtils beUtils = new BaseEntityUtils();
//
//        QMessageFactory qMessageFactory = new QMessageFactory();
//
//        BaseEntity beRecipient = jsonb.fromJson(searchJson, BaseEntity.class);
//
//        qMessageFactory.getMessageProvider(QBaseMSGMessageType.EMAIL);
//
//        if (beRecipient != null) {
//            /* Init Sendgrid to Agent Msg */
//            QCommsMessage msg = new QCommsMessage.Builder("MSG_AGT2_ASSIGNED")
//                    .addRecipient(beRecipient)
////                    .addContext("INTERN", newBe)
////                    .addContext("URL:ENCODE", "SBE_INTERNS"+userToken.getJTI().toUpperCase()+"/ACT_PRI_EVENT_VIEW/"+newBe.getCode())
//                    .setUtils(beUtils)
//                    .send();
//        }
//
////        System.out.println("Simple test " + be);
//    }
//}
