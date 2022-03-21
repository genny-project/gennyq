package life.genny.kogito.live.data;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.annotations.Blocking;

import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.HttpUtils;
import life.genny.qwandaq.utils.KogitoUtils;

@ApplicationScoped
public class InternalConsumer {

    static final Logger log = Logger.getLogger(InternalConsumer.class);

    static Jsonb jsonb = JsonbBuilder.create();

    @Inject
    KogitoUtils kogitoUtils;

    @ConfigProperty(name = "kogito.service.url", defaultValue = "http://alyson.genny.life:8250")
    String myUrl;

    // @Inject
    // KieRuntimeBuilder ruleRuntime;

    // @Inject
    // Service service;

    // KieSession ksession;

    /**
     * Execute on start up.
     *
     * @param ev
     */
    void onStart(@Observes StartupEvent ev) {

        // service.fullServiceInit();
        log.info("[*] Finished Events Startup!");
    }

    /**
     * Consume from the valid_data topic.
     *
     * @param data
     */
    @Incoming("events")
    @Blocking
    public void getEvent(String data) {

        log.info("Incoming Event :" + data);
        Instant start = Instant.now();

        GennyToken gToken = null;
        // Convert to Json and identify the application
        JsonObject eventJson = jsonb.fromJson(data, JsonObject.class);
        if (eventJson.containsKey("token")) {
            String tokenStr = eventJson.getString("token");
            // log.info("token=" + tokenStr);
            gToken = new GennyToken(tokenStr);
            log.info("Token username " + gToken.getUsername());

            if (eventJson.containsKey("data")) {
                JsonObject dataJson = eventJson.getJsonObject("data");
                if (dataJson.containsKey("code")) {
                    String code = dataJson.getString("code");
                    if ("ACT_PRI_EVENT_APPLY".equals(code)) {
                        String targetCode = dataJson.getString("targetCode");
                        log.info("Intern APPLY - targetCode:" + targetCode);
                        String internCode = dataJson.getString("targetCode");
                        String agentCode = gToken.getUserCode();
                        String appParms = "{\"internCode\":\"" + internCode + "\",\"agentCode\":\"" + agentCode
                                + "\" }";
                        log.info("appParms=" + appParms);
                        // trigger the application workflow
                        String url = myUrl + "/application";
                        HttpResponse<String> response = HttpUtils.post(url, appParms, gToken.getToken());
                        int responseCode = response.statusCode();
                        if (responseCode == 201) {

                            JsonObject idJson = jsonb.fromJson(response.body(), JsonObject.class);
                            String id = idJson.getString("id");
                            log.info("processId = " + id);

                            String test = kogitoUtils.fetchGraphQL("Application", "internCode", "PER_A%",
                                    gToken.getToken(), "id", "internCode");
                            log.info(test);
                        } else {
                            log.error(response.statusCode());
                        }
                    } else if ("ACT_PRI_EVENT_VIEW".equals(code)) {
                        // Now signal the process
                        String targetCode = dataJson.getString("targetCode");
                        log.info("Intern VIEW - targetCode:" + targetCode);
                        String internCode = dataJson.getString("targetCode");

                        String test = kogitoUtils.fetchGraphQL("Application", "internCode", internCode,
                                gToken.getToken(), "id", "internCode");
                        log.info(test);
                        String sourceCode = gToken.getUserCode();
                        if ("PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943".equals(sourceCode)) {
                            try {
                                String processId = kogitoUtils.fetchProcessId("Application", "internCode",
                                        internCode,
                                        gToken.getToken()); // fetchProcessId("Application", "internCode",
                                                                                                                                                   // internCode,
                                                                                                                                                   // gToken.getToken());
                                                                                                                                                   // Send signal
                                log.info("ProcessId=" + processId);
                                String result = kogitoUtils.sendSignal("Application", processId, "ARCHIVE",
                                        gToken.getToken());
                                log.info(result);
                            } catch (Exception e) {
                                log.info(e.getLocalizedMessage());
                            }
                        }

                    }
                } else {

                }
            }
        }

        // {}{"data":{"sourceCode":"PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943","targetCode":"APP_77BEB28D-FCAB-4A3A-B59E-4BFA87E3EC0E","value":"","parentCode":"QUE_ADD_APPLICATION_GRP","questionCode":"QUE_SELECT_HOST_COMPANY","code":"QUE_SELECT_HOST_COMPANY"},"token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJwU180dHhEMTJpUVJIZlJaLURRLTFaRWlGS3pWYkttVVFFWjdqOUdPaTZVIn0.eyJleHAiOjE2NTAyNzIyNzUsImlhdCI6MTY0NzY4MDI3NSwiYXV0aF90aW1lIjoxNjQ3Njc0NDAxLCJqdGkiOiI0ZGM1ZDM1NC1kYTNlLTRhN2EtODNkMi1hMTMzMGQzZGY4N2YiLCJpc3MiOiJodHRwczovL2tleWNsb2FrLmdhZGEuaW8vYXV0aC9yZWFsbXMvaW50ZXJubWF0Y2giLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiMDg2Y2RmMWYtYTk4Zi00ZTczLTk4MjUtMGE0Y2ZlMmJiOTQzIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYWx5c29uIiwibm9uY2UiOiIxNTE2NTFlNS00ZGQ1LTQ1ZjEtYjgzMS1iYjMzYmM2Yzk0ZmIiLCJzZXNzaW9uX3N0YXRlIjoiZTc5YTNlODItMTA1Yi00MGRkLTgzMzItNDE0ZWEwZmI3YTAyIiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwczovL2ludGVybm1hdGNoLXN0YWdpbmcuZ2FkYS5pbyIsImh0dHBzOi8vaW50ZXJubWF0Y2gtZGV2LmdhZGEuaW8iLCJodHRwczovL2ludGVybm1hdGNoLWludGVybnMuZ2FkYS5pbyIsImh0dHBzOi8vaW50ZXJubWF0Y2gtc3RhZ2luZzIuZ2FkYS5pbyIsImh0dHBzOi8vaW50ZXJubWF0Y2gtZGV2Mi5nYWRhLmlvIiwiaHR0cHM6Ly9tLmludGVybm1hdGNoLmlvIiwiaHR0cHM6Ly9pbnRlcm5tYXRjaC5nZW5ueS5saWZlIiwiaHR0cHM6Ly9pbnRlcm5tYXRjaC1waXBlcy5nYWRhLmlvIiwiaHR0cHM6Ly9pbnRlcm5tYXRjaC1tb2JpbGUuZ2FkYS5pbyIsImh0dHA6Ly9sb2NhbGhvc3Q6MzAwMCIsImh0dHA6Ly8xOTIuMTY4LjE3LjExNjozMDAwIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJwdGVzdCIsImIyYiIsImRldiIsInRlc3QiLCJvZmZsaW5lX2FjY2VzcyIsImFkbWluIiwiSFIiLCJ1bWFfYXV0aG9yaXphdGlvbiIsIklUIiwidXNlciIsInN1cGVydmlzb3IiLCJtYW5hZ2VycyJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJzaWQiOiJlNzlhM2U4Mi0xMDViLTQwZGQtODMzMi00MTRlYTBmYjdhMDIiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6IlRlc3R1c2VyIEFseXNvbiIsInByZWZlcnJlZF91c2VybmFtZSI6InRlc3R1c2VyQGdhZGEuaW8iLCJnaXZlbl9uYW1lIjoiVGVzdHVzZXIiLCJmYW1pbHlfbmFtZSI6IkFseXNvbiIsImVtYWlsIjoidGVzdHVzZXJAZ2FkYS5pbyJ9.RP3j6ICwIdrFCeaILVYO-Yd1T2XwvHTmY6GxGQve-Z2XzOIPY_Xu_UkjHGWVAe6-ivI52WBRgCNMQubOZbLw1Mz2I8DQt6D0gGNhML9lXx-OR_9Ek5pudWoRvQ3G8LkomAyJsciFyPuAhbSjLcs4Nia8tKnCEnPeiZtbo7uMiaqZ-tbnhE3-Icx5BdPUSb5IRSKE7pIjENdwwvuq18r-TWkkJLxERpxq092mgcuPtnyFVe9ZGrUMU9yIPyD58XnKUBMsq50rbWgcUskgCufgYS_ipRdL0ONhVCySRF-ZQfx8J_nLB482mQ0rhCSQUFtDbjymugd8tqJgiG8XoVQgQw","msg_type":"EVT_MSG","event_type":"DD","redirect":false,"attributeCode":"LNK_HOST_COMPANY","questionCode":"QUE_SELECT_HOST_COMPANY","code":"QUE_SELECT_HOST_COMPANY","4dc5d354-da3e-4a7a-83d2-a1330d3df87f":"0bd43d11-ada4-4770-9c9d-94f1fc63cd23"}

        // BaseEntityUtils beUtils = service.getBeUtils();
        // GennyToken serviceToken = beUtils.getServiceToken();
        // GennyToken userToken = null;

        // // deserialise to msg
        // QDataAnswerMessage msg = jsonb.fromJson(data, QDataAnswerMessage.class);

        // // check the token
        // String token = msg.getToken();
        // try {
        // userToken = new GennyToken(token);
        // } catch (Exception e) {
        // log.error("Invalid Token!");
        // return;
        // }

        // // update the token of our utility
        // beUtils.setGennyToken(userToken);

        // Answer answer = msg.getItems()[0];
        // Answers answersToSave = new Answers();

        // // init session and activate DataProcessing
        // KieSession ksession = ruleRuntime.newKieSession();
        // ((InternalAgenda)
        // ksession.getAgenda()).activateRuleFlowGroup("DataProcessing");

        // // insert facts into session
        // ksession.insert(beUtils);
        // ksession.insert(serviceToken);
        // ksession.insert(userToken);
        // ksession.insert(answer);
        // ksession.insert(answersToSave);

        // // fire rules and dispose of session
        // ksession.fireAllRules();
        // ksession.dispose();

        // // TODO: ensure that answersToSave has been updated by our rules
        // beUtils.saveAnswers(answersToSave);

        Instant end = Instant.now();
        log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
    }

    /**
     * Consume from the valid_data topic.
     *
     * @param data
     */
    @Incoming("valid_data")
    @Blocking
    public void getData(String data) {
        // Convert to Json and identify the application
        JsonObject dataJson = jsonb.fromJson(data, JsonObject.class);
        if (dataJson.containsKey("items")) {
            JsonArray itemsArray = dataJson.getJsonArray("items");
            if (!itemsArray.isEmpty()) {

                JsonObject item0 = itemsArray.getJsonObject(0);
                String item0Str = item0.toString();
                // log.info("item0=" + item0Str);
                if ("{}".equals(item0Str)) {
                    log.info("Alyson Heartbeat");
                    return;
                }
            }
        }
        log.info("Incoming Data :" + data);
        Instant start = Instant.now();

        Instant end = Instant.now();
        log.info("Duration = " + Duration.between(start, end).toMillis() + "ms");
    }

}
