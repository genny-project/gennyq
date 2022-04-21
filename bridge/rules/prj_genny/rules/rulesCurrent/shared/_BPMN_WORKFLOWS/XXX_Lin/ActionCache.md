# Action Cache - Toggle Frame

## Procedure

The Action Cache is for sending a Vertx message to the frontend frame to trigger frame toggle event.

Related frame contents

1. pri_content
1. pri_content_closed

The signal should attached the theme with frame once initialised frame at the frontend with following elements.

1. Question. code, code of the button
1. setAttributes, FRM_HEADER, Frame Builder

** Remarks: details of the frame procedure will cover by other procedure.

## Build a BPMN workflows

1. Create a new BPMN2 file call `ActionCache.bpmn`, then in the Process Diagram property,
    1. at "Process" tag,
        1. "Id" fill in `ActionCacheProcessID`
        2. "Name" fill in `ActionCacheProcessName`

    2. "Definitions" tag,
        1. "Imports" panel add life.genny.qwanda.utils.OutputParam;
        2. "Imports" panel add life.genny.models.GennyToken
        3. "Imports" panel add life.genny.qwanda.message.QEventMessage
        4. "Imports" panel add life.genny.qwanda.message.QCmdMessageToggleFrame
        5. "Imports" panel add life.genny.utils.VertxUtils
        6. "Imports" panel add import life.genny.qwandautils.JsonUtils
        7. "Signal List" add `SignalToggleFrame`, the signal name will be allow outside call in.

    3. "Data Items" tag, "Local Variable List" panel add

        1. `outputMsg` -> life.genny.qwanda.utils.OutputParam;;
        2. `userToken` -> life.genny.models.GennyToken
        3. `serviceToken` -> life.genny.models.GennyToken
        4. `qMessage` -> life.genny.qwanda.message.QEventMessage

2. At the "Start Event" property insert a signal listener

    1. at "Event" tag add Event Type "Signal Event Definition" and Event Details `SignalToggleFrame` which was passed in by the test case

3. "RunRuleFlowGroup" property, which is the protocol that for communicate with the Drools, which can pass or receive data between BPMN and Drools.

    1. In the "I/O Parameters" tag,

        1. "Output Data Mapping" add a new mapping from name "output", Data Type "life.genny.qwanda.utils.OutputParam" -> To Data Item `outputMsg`, the from keyword "output" is the system default variable which cannot change, "outputMsg" declare an object that will content the output object from Drools, then allow BPMN process to reuse
        2. "Input Data Mapping" add From Data Type "qMessage" To Name "qMessage" Data Type "life.genny.qwanda.message.QEventMessage"

    2. In the "Custom Task" tag

        1. add the rule group name Expression value `"RulesPostitionTemplateDefault"` into "Rule Flow Group", the group name must be quoted by `"`
        2. add Data Item `serviceToken` for "Service Token"
        3. add Data Item `userToken` for "User Token"

    3. To allow display Drools return output in workflow, in "Custom Task" tag, "On Exit Script" panel, print out outputMsg `System.out.println(outputMsg);`.

## Build a Drools File

create a new file call `ActionCache.drl`, then paste following codes into the file.

```java
package life.genny.rules;
import life.genny.qwanda.message.QEventMessage;
import life.genny.models.GennyToken;
import life.genny.utils.VertxUtils;
import io.vertx.core.json.JsonObject;
import life.genny.qwanda.utils.OutputParam;

rule "DEFAULT 1"
    ruleflow-group 'RulesActionCacheDefault'
    lock-on-active
    salience -10000
    no-loop true
    when
        userToken : GennyToken ( )
        //serviceToken : GennyToken (code == "PER_SERVICE" )
        output : OutputParam( )
        qMessage : QEventMessage( data.code == "actionCacheLayoutMessage" )
    then
        System.out.println("RulesActionCache - DEFAULT 1 - Triggerred : " + userToken.getUserCode());
        System.out.println("Signal Content: " + qMessage);
        output.setTypeOfResult("NONE");
        output.setResultCode("InstructFrontendToggleFrame");
        output.setTargetCode("");
end
```

## Build a JUnit Test File

create a new file call `LinTestActionCache.java`, following codes are the example test case, please refer to the Git repo for the latest.

Following is Test Configuration values

```bash
DDT_URL=http://alyson7.genny.life
FORCE_CACHE_USE_API=TRUE
FORCE_EVENTBUS_USE_API=TRUE
PASSWORD=WelcomeToTheHub121!
PROJECT_REALM=internmatch
PROJECT_URL=http://alyson7.genny.life
REACT_APP_QWANDA_API_URL=http://alyson7.genny.life
RULES_DIR=./rules
USERNAME=user1
```

Following are the jUnit Test case

```java
package life.genny.test;

import java.io.FileNotFoundException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.springframework.jms.core.JmsTemplate;

import life.genny.eventbus.EventBusInterface;
import life.genny.eventbus.EventBusMock;
import life.genny.eventbus.VertxCache;
import life.genny.models.GennyToken;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwandautils.GennyCacheInterface;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.rules.QRules;
import life.genny.utils.VertxUtils;

/**
 * 
 * @author Dominic Lin
 * 
 * 
 *         This test class will test Template Lifecycle by using assertions.
 *
 */
public class LinTestActionCache extends JbpmJUnitBaseTestCase {

    protected static final Logger log = org.apache.logging.log4j.LogManager
	    .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    protected static String realm = GennySettings.mainrealm;
    protected static Set<String> realms;

    protected static EventBusInterface eventBusMock;
    protected static GennyCacheInterface vertxCache;

    /* Constant(s): */
    public static final String AMQ_BROKER_URL = "tcp://localhost:61616";
    public static final String QUEUE_NAME = "queue/KIE.SERVER.EXECUTOR";

    /* Instance variable(s): */
    protected ConnectionFactory mActiveMQConnectionFactory;
    protected JmsTemplate mJmsTemplate;

    // Constructor
    public LinTestActionCache() {

    }

    @BeforeClass
    public static void init() throws FileNotFoundException, SQLException {

	System.out.println("BridgeUrl=" + GennySettings.bridgeServiceUrl);
	System.out.println("QwandaUrl=" + GennySettings.qwandaServiceUrl);

	// Set up realm
	realms = new HashSet<String>();
	realms.add(realm);
	realms.stream().forEach(System.out::println);
	realms.remove("genny");

	// Enable the PseudoClock using the following system property.
	System.setProperty("drools.clockType", "pseudo");

	eventBusMock = new EventBusMock();
	vertxCache = new VertxCache(); // MockCache
	VertxUtils.init(eventBusMock, vertxCache);
    }

    @Before
    public void setUp() {
	mActiveMQConnectionFactory = new ActiveMQConnectionFactory(AMQ_BROKER_URL);
	mJmsTemplate = new JmsTemplate(mActiveMQConnectionFactory);
	final Destination theTestDestination = new ActiveMQQueue(QUEUE_NAME);
	mJmsTemplate.setDefaultDestination(theTestDestination);
	mJmsTemplate.setReceiveTimeout(500L);
    }

    @Test
    public void testActionCacheMessageBySignal() {

	System.out.println("Start Action Cache Test");
	GennyToken userToken = null;
	GennyToken serviceToken = null;
	QRules qRules = null;
	boolean isCreateToken = true;

	if (isCreateToken) {
	    userToken = GennyJbpmBaseTest.createGennyToken(realm, "user13", "Barry Allan", "user");
	    serviceToken = GennyJbpmBaseTest.createGennyToken(realm, "service", "Service User", "service");
	    qRules = new QRules(eventBusMock, userToken.getToken());
	    qRules.set("realm", userToken.getRealm());
	    qRules.setServiceToken(serviceToken.getToken());
	    VertxUtils.cachedEnabled = true; // don't send to local Service Cache
	} else {
	    qRules = GennyJbpmBaseTest.setupLocalService();
	    userToken = new GennyToken("userToken", qRules.getToken());
	    serviceToken = new GennyToken("PER_SERVICE", qRules.getServiceToken());
	}

	System.out.println("session     =" + userToken.getJTI());
	System.out.println("userToken   =" + userToken.getToken());
	System.out.println("serviceToken=" + serviceToken.getToken());

	// This will pass into the Drools rule
	QEventMessage actionCacheLayoutMessage = new QEventMessage("EVT_LAYOUT", "actionCacheLayoutMessage");

	QEventMessage authInitMsg1 = new QEventMessage("EVT_MSG", "AUTH_INIT");
	authInitMsg1.setToken(userToken.getToken());

	QEventMessage msgLogout1 = new QEventMessage("EVT_MSG", "LOGOUT");
	msgLogout1.setToken(userToken.getToken());

	// NOW SET UP Some baseentitys
	BaseEntity project = new BaseEntity("PRJ_" + serviceToken.getRealm().toUpperCase(),
		StringUtils.capitaliseAllWords(serviceToken.getRealm()));
	project.setRealm(serviceToken.getRealm());
	VertxUtils.writeCachedJson(serviceToken.getRealm(), "PRJ_" + serviceToken.getRealm().toUpperCase(),
		JsonUtils.toJson(project), serviceToken.getToken());

	GennyKieSession gks = null;

	System.setProperty("org.kie.executor.jms", "false");
	try {
	    gks = GennyKieSession.builder(serviceToken, true)//
		    .addDrl("ActionCache.drl")//
		    .addJbpm("ActionCache.bpmn")//
		    .addToken(userToken)//
		    .build();

	    gks.start();

	    gks.injectEvent(authInitMsg1); // This should create a new process
	    gks.advanceSeconds(5, false);

	    // Maunally trigger the workflow
//	    gks.startProcess("ActionCacheProcessID");

	    // This will inject into all started workflows
	    // with a signal call "SignalToggleFrame" and event object
	    // The workflow will fire automatically after receive the signal
	    gks.injectSignal("SignalToggleFrame", actionCacheLayoutMessage);
	    gks.advanceSeconds(5, false);

	    //Trigger the Logout Event
	    gks.injectEvent(msgLogout1);
	} catch (Exception e) {
	    e.printStackTrace();
	} finally {
	    if (gks != null) {
		gks.close();
	    }
	}

    }

//    @Test
    public void testWorkflowNodeTriggerred() {
	RuntimeManager manager = createRuntimeManager(
		"rulesCurrent/shared/_BPMN_WORKFLOWS/XXX_Lin/ActionCache.bpmn");
	RuntimeEngine engine = getRuntimeEngine(null);
	KieSession ksession = engine.getKieSession();

	// This will looking at the id of the workflow
	ProcessInstance processInstance = ksession.startProcess("ActionCacheProcessID");

	// Examinate the Delete Company Flow by nodes
	assertNodeTriggered(processInstance.getId(), "startActionCacheEvent");
	assertNodeTriggered(processInstance.getId(), "RunRuleFlowGroup");
	assertNodeTriggered(processInstance.getId(), "endActionCacheEvent");
//	assertNodeTriggered(processInstance.getId(), "is delete Template?");

	manager.disposeRuntimeEngine(engine);
	manager.close();
    }
}


```

~ End of Procedure ~
