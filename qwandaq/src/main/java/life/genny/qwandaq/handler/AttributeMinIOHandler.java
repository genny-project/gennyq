package life.genny.qwandaq.handler;

import io.quarkus.arc.Arc;
import life.genny.qwandaq.constants.QwandaQConstant;
import life.genny.qwandaq.dto.FileUpload;
import life.genny.qwandaq.exception.runtime.AttributeMinIOException;
import life.genny.qwandaq.utils.ConfigUtils;
import life.genny.qwandaq.utils.MinIOUtils;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AttributeMinIOHandler {
    private static final Logger log = Logger.getLogger(AttributeMinIOHandler.class);

    public static String convertToMinIOObject(String valueString, String baseEntityCode, String attributeCode) {
        try {
            int limit = ConfigUtils.getConfig("attribute.minio.threshold", Integer.class) * 1024; // 4Kb
            // logic to push to minIO if it is greater than certain size
            byte[] data = valueString.getBytes(StandardCharsets.UTF_8);
            if (data.length > limit) {
                log.info("Converting attribute as MinIO Object");
                String fileName = QwandaQConstant.MINIO_LAZY_PREFIX + baseEntityCode + "-" + attributeCode;
                String path = ConfigUtils.getConfig("file.temp", String.class);
                File theDir = new File(path);
                if (!theDir.exists()) {
                    theDir.mkdirs();
                }
                String fileInfoName = path.concat(fileName);
                File fileInfo = new File(fileInfoName);
                try (FileWriter myWriter = new FileWriter(fileInfo.getPath())) {
                    myWriter.write(valueString);
                } catch (IOException e) {
                    new AttributeMinIOException("Converting to MinIO Object failed", e.fillInStackTrace()).printStackTrace();
                    return valueString;
                }
                log.info("Writing to MinIO");

                fileInfo.delete();

                return Arc.container().instance(MinIOUtils.class).get().saveOnStore(new FileUpload(fileName, fileInfoName));
            } else {
                return valueString;
            }
        } catch (Exception ex) {
            new AttributeMinIOException("Converting to MinIO Object failed", ex.fillInStackTrace()).printStackTrace();
            return valueString;
        }
    }
}


/*life.genny.qwandaq.exception.runtime.AttributeMinIOException: Converting to MinIO Object failed
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090034285Z         at life.genny.qwandaq.handler.AttributeMinIOHandler.convertToMinIOObject(AttributeMinIOHandler.java:49)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090051206Z         at life.genny.qwandaq.attribute.EntityAttribute.autocreateUpdate(EntityAttribute.java:619)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090059316Z         at jdk.internal.reflect.GeneratedMethodAccessor139.invoke(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090065616Z         at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090071816Z         at java.base/java.lang.reflect.Method.invoke(Method.java:566)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090077466Z         at org.hibernate.jpa.event.internal.EntityCallback.performCallback(EntityCallback.java:50)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090098417Z         at org.hibernate.jpa.event.internal.CallbackRegistryImpl.callback(CallbackRegistryImpl.java:97)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090105977Z         at org.hibernate.jpa.event.internal.CallbackRegistryImpl.preUpdate(CallbackRegistryImpl.java:71)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090111717Z         at org.hibernate.event.internal.DefaultFlushEntityEventListener.invokeInterceptor(DefaultFlushEntityEventListener.java:369)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090117587Z         at org.hibernate.event.internal.DefaultFlushEntityEventListener.handleInterception(DefaultFlushEntityEventListener.java:351)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090123277Z         at org.hibernate.event.internal.DefaultFlushEntityEventListener.scheduleUpdate(DefaultFlushEntityEventListener.java:302)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090128717Z         at org.hibernate.event.internal.DefaultFlushEntityEventListener.onFlushEntity(DefaultFlushEntityEventListener.java:171)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090134507Z         at org.hibernate.event.service.internal.EventListenerGroupImpl.fireEventOnEachListener(EventListenerGroupImpl.java:107)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090140257Z         at org.hibernate.event.internal.AbstractFlushingEventListener.flushEntities(AbstractFlushingEventListener.java:229)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090157358Z         at org.hibernate.event.internal.AbstractFlushingEventListener.flushEverythingToExecutions(AbstractFlushingEventListener.java:93)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090164048Z         at org.hibernate.event.internal.DefaultFlushEventListener.onFlush(DefaultFlushEventListener.java:39)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090169078Z         at org.hibernate.event.service.internal.EventListenerGroupImpl.fireEventOnEachListener(EventListenerGroupImpl.java:107)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090174598Z         at org.hibernate.internal.SessionImpl.doFlush(SessionImpl.java:1407)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090180118Z         at org.hibernate.internal.SessionImpl.managedFlush(SessionImpl.java:489)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090185818Z         at org.hibernate.internal.SessionImpl.flushBeforeTransactionCompletion(SessionImpl.java:3290)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090191428Z         at org.hibernate.internal.SessionImpl.beforeTransactionCompletion(SessionImpl.java:2425)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090196908Z         at org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl.beforeTransactionCompletion(JdbcCoordinatorImpl.java:449)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090202469Z         at org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl.beforeCompletion(JtaTransactionCoordinatorImpl.java:356)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090208329Z         at org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackCoordinatorNonTrackingImpl.beforeCompletion(SynchronizationCallbackCoordinatorNonTrackingImpl.java:47)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090225759Z         at org.hibernate.resource.transaction.backend.jta.internal.synchronization.RegisteredSynchronization.beforeCompletion(RegisteredSynchronization.java:37)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090234019Z         at com.arjuna.ats.internal.jta.resources.arjunacore.SynchronizationImple.beforeCompletion(SynchronizationImple.java:76)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090238369Z         at com.arjuna.ats.arjuna.coordinator.TwoPhaseCoordinator.beforeCompletion(TwoPhaseCoordinator.java:360)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090243249Z         at com.arjuna.ats.arjuna.coordinator.TwoPhaseCoordinator.end(TwoPhaseCoordinator.java:91)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090258650Z         at com.arjuna.ats.arjuna.AtomicAction.commit(AtomicAction.java:162)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090265350Z         at com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple.commitAndDisassociate(TransactionImple.java:1295)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090270900Z         at com.arjuna.ats.internal.jta.transaction.arjunacore.BaseTransaction.commit(BaseTransaction.java:128)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090275470Z         at io.quarkus.narayana.jta.runtime.CDIDelegatingTransactionManager.commit(CDIDelegatingTransactionManager.java:105)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090279200Z         at io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorBase.endTransaction(TransactionalInterceptorBase.java:374)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090282940Z         at io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorBase.invokeInOurTx(TransactionalInterceptorBase.java:170)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090335071Z         at io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorBase.invokeInOurTx(TransactionalInterceptorBase.java:104)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090374272Z         at io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorRequired.doIntercept(TransactionalInterceptorRequired.java:38)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090383392Z         at io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorBase.intercept(TransactionalInterceptorBase.java:58)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090390192Z         at io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorRequired.intercept(TransactionalInterceptorRequired.java:32)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090396892Z         at io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorRequired_Bean.intercept(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090403372Z         at io.quarkus.arc.impl.InterceptorInvocation.invoke(InterceptorInvocation.java:41)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090409552Z         at io.quarkus.arc.impl.AroundInvokeInvocationContext.perform(AroundInvokeInvocationContext.java:40)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090415342Z         at io.quarkus.arc.impl.InvocationContexts.performAroundInvoke(InvocationContexts.java:32)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090421013Z         at life.genny.qwandaq.utils.DatabaseUtils_Subclass.saveBaseEntity(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090431173Z         at life.genny.qwandaq.utils.DatabaseUtils_ClientProxy.saveBaseEntity(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090501074Z         at life.genny.qwandaq.utils.BaseEntityUtils.updateBaseEntity(BaseEntityUtils.java:261)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090518834Z         at life.genny.qwandaq.utils.BaseEntityUtils_Subclass.updateBaseEntity$$superforward1(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090525745Z         at life.genny.qwandaq.utils.BaseEntityUtils_Subclass$$function$$29.apply(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090531795Z         at io.quarkus.arc.impl.AroundInvokeInvocationContext.proceed(AroundInvokeInvocationContext.java:53)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090538215Z         at io.quarkus.arc.impl.ActivateRequestContextInterceptor.invoke(ActivateRequestContextInterceptor.java:122)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090543875Z         at io.quarkus.arc.impl.ActivateRequestContextInterceptor.aroundInvoke(ActivateRequestContextInterceptor.java:31)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090576225Z         at io.quarkus.arc.impl.ActivateRequestContextInterceptor_Bean.intercept(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090585226Z         at io.quarkus.arc.impl.InterceptorInvocation.invoke(InterceptorInvocation.java:41)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090591676Z         at io.quarkus.arc.impl.AroundInvokeInvocationContext.perform(AroundInvokeInvocationContext.java:40)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090597646Z         at io.quarkus.arc.impl.InvocationContexts.performAroundInvoke(InvocationContexts.java:32)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090616746Z         at life.genny.qwandaq.utils.BaseEntityUtils_Subclass.updateBaseEntity(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090622446Z         at life.genny.qwandaq.utils.BaseEntityUtils.updateBaseEntity(BaseEntityUtils.java:238)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090627996Z         at life.genny.qwandaq.utils.BaseEntityUtils_Subclass.updateBaseEntity$$superforward1(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090633447Z         at life.genny.qwandaq.utils.BaseEntityUtils_Subclass$$function$$30.apply(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090656447Z         at io.quarkus.arc.impl.AroundInvokeInvocationContext.proceed(AroundInvokeInvocationContext.java:53)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090663497Z         at io.quarkus.arc.impl.ActivateRequestContextInterceptor.invoke(ActivateRequestContextInterceptor.java:122)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090667537Z         at io.quarkus.arc.impl.ActivateRequestContextInterceptor.aroundInvoke(ActivateRequestContextInterceptor.java:31)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090671227Z         at io.quarkus.arc.impl.ActivateRequestContextInterceptor_Bean.intercept(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090674757Z         at io.quarkus.arc.impl.InterceptorInvocation.invoke(InterceptorInvocation.java:41)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090678257Z         at io.quarkus.arc.impl.AroundInvokeInvocationContext.perform(AroundInvokeInvocationContext.java:40)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090681767Z         at io.quarkus.arc.impl.InvocationContexts.performAroundInvoke(InvocationContexts.java:32)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090685237Z         at life.genny.qwandaq.utils.BaseEntityUtils_Subclass.updateBaseEntity(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090688788Z         at life.genny.qwandaq.utils.BaseEntityUtils_ClientProxy.updateBaseEntity(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090694238Z         at life.genny.kogito.common.service.BaseEntityService.setActive(BaseEntityService.java:163)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090697898Z         at life.genny.kogito.common.service.BaseEntityService_ClientProxy.setActive(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090701668Z         at org.kie.kogito.handlers.BaseEntityService_setActive_1_Handler.executeWorkItem(BaseEntityService_setActive_1_Handler.java:20)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090705528Z         at org.kie.kogito.handlers.BaseEntityService_setActive_1_Handler_ClientProxy.executeWorkItem(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090709078Z         at org.jbpm.process.instance.LightWorkItemManager.internalExecuteWorkItem(LightWorkItemManager.java:79)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090713778Z         at org.jbpm.workflow.instance.node.WorkItemNodeInstance.lambda$internalTrigger$0(WorkItemNodeInstance.java:161)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090717438Z         at org.jbpm.workflow.instance.node.WorkItemNodeInstance.processWorkItemHandler(WorkItemNodeInstance.java:174)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090732708Z         at org.jbpm.workflow.instance.node.WorkItemNodeInstance.internalTrigger(WorkItemNodeInstance.java:160)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090791699Z         at org.jbpm.workflow.instance.impl.NodeInstanceImpl.trigger(NodeInstanceImpl.java:225)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090809340Z         at org.jbpm.workflow.instance.impl.NodeInstanceImpl.triggerNodeInstance(NodeInstanceImpl.java:424)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090815440Z         at org.jbpm.workflow.instance.impl.NodeInstanceImpl.triggerNodeInstance(NodeInstanceImpl.java:409)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090821090Z         at org.jbpm.workflow.instance.impl.NodeInstanceImpl.triggerCompleted(NodeInstanceImpl.java:379)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090826690Z         at org.jbpm.workflow.instance.impl.ExtendedNodeInstanceImpl.triggerCompleted(ExtendedNodeInstanceImpl.java:51)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090832180Z         at org.jbpm.workflow.instance.node.StateBasedNodeInstance.triggerCompleted(StateBasedNodeInstance.java:336)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090837840Z         at org.jbpm.workflow.instance.node.StateBasedNodeInstance.triggerCompleted(StateBasedNodeInstance.java:296)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090843320Z         at org.jbpm.workflow.instance.node.LambdaSubProcessNodeInstance.processInstanceCompleted(LambdaSubProcessNodeInstance.java:201)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090849010Z         at org.jbpm.workflow.instance.node.LambdaSubProcessNodeInstance.signalEvent(LambdaSubProcessNodeInstance.java:161)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090933072Z         at org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl.signalEvent(WorkflowProcessInstanceImpl.java:626)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090947992Z         at org.kie.kogito.process.impl.AbstractProcessInstance.send(AbstractProcessInstance.java:324)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090952392Z         at org.kie.kogito.process.impl.AbstractProcess$CompletionEventListener.lambda$signalEvent$0(AbstractProcess.java:284)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090956042Z         at java.base/java.util.Optional.ifPresent(Optional.java:183)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090959643Z         at org.kie.kogito.process.impl.AbstractProcess$CompletionEventListener.signalEvent(AbstractProcess.java:284)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090963943Z         at org.kie.kogito.services.signal.LightSignalManager.lambda$signalEvent$2(LightSignalManager.java:65)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090968283Z         at java.base/java.util.concurrent.CopyOnWriteArrayList.forEach(CopyOnWriteArrayList.java:807)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090973683Z         at org.kie.kogito.services.signal.LightSignalManager.signalEvent(LightSignalManager.java:65)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090978923Z         at org.kie.kogito.services.signal.DefaultSignalManagerHub.publish(DefaultSignalManagerHub.java:42)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090984263Z         at org.kie.kogito.services.signal.LightSignalManager.signalEvent(LightSignalManager.java:68)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090989543Z         at org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl.setState(WorkflowProcessInstanceImpl.java:434)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.090994823Z         at org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl.setState(WorkflowProcessInstanceImpl.java:443)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091011964Z         at org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl.nodeInstanceCompleted(WorkflowProcessInstanceImpl.java:895)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091031784Z         at org.jbpm.workflow.instance.node.EndNodeInstance.internalTrigger(EndNodeInstance.java:77)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091038864Z         at org.jbpm.workflow.instance.impl.NodeInstanceImpl.trigger(NodeInstanceImpl.java:225)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091044374Z         at org.jbpm.workflow.instance.impl.NodeInstanceImpl.triggerNodeInstance(NodeInstanceImpl.java:424)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091049954Z         at org.jbpm.workflow.instance.impl.NodeInstanceImpl.triggerNodeInstance(NodeInstanceImpl.java:409)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091056514Z         at org.jbpm.workflow.instance.impl.NodeInstanceImpl.triggerCompleted(NodeInstanceImpl.java:379)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091060574Z         at org.jbpm.workflow.instance.impl.ExtendedNodeInstanceImpl.triggerCompleted(ExtendedNodeInstanceImpl.java:51)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091064174Z         at org.jbpm.workflow.instance.node.StateBasedNodeInstance.triggerCompleted(StateBasedNodeInstance.java:336)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091067795Z         at org.jbpm.workflow.instance.node.StateBasedNodeInstance.triggerCompleted(StateBasedNodeInstance.java:296)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091136596Z         at org.jbpm.workflow.instance.node.WorkItemNodeInstance.triggerCompleted(WorkItemNodeInstance.java:272)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091152156Z         at org.jbpm.workflow.instance.node.WorkItemNodeInstance.workItemCompleted(WorkItemNodeInstance.java:357)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091158286Z         at org.jbpm.workflow.instance.node.WorkItemNodeInstance.signalEvent(WorkItemNodeInstance.java:325)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091163866Z         at org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl.signalEvent(WorkflowProcessInstanceImpl.java:620)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091169216Z         at org.jbpm.process.instance.LightWorkItemManager.internalCompleteWorkItem(LightWorkItemManager.java:183)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091176997Z         at org.jbpm.process.instance.LightWorkItemManager.transitionWorkItem(LightWorkItemManager.java:214)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091183377Z         at org.jbpm.process.instance.LightWorkItemManager.transitionWorkItem(LightWorkItemManager.java:193)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091253028Z         at org.jbpm.process.instance.LightWorkItemManager.completeWorkItem(LightWorkItemManager.java:154)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091267388Z         at org.kie.kogito.handlers.Service2Service_initialiseScope_1_Handler.executeWorkItem(Service2Service_initialiseScope_1_Handler.java:21)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091274218Z         at org.kie.kogito.handlers.Service2Service_initialiseScope_1_Handler_ClientProxy.executeWorkItem(Unknown Source)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091280258Z         at org.jbpm.process.instance.LightWorkItemManager.internalExecuteWorkItem(LightWorkItemManager.java:79)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091285789Z         at org.jbpm.workflow.instance.node.WorkItemNodeInstance.lambda$internalTrigger$0(WorkItemNodeInstance.java:161)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091291529Z         at org.jbpm.workflow.instance.node.WorkItemNodeInstance.processWorkItemHandler(WorkItemNodeInstance.java:174)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091376150Z         at org.jbpm.workflow.instance.node.WorkItemNodeInstance.internalTrigger(WorkItemNodeInstance.java:160)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091408821Z         at org.jbpm.workflow.instance.impl.NodeInstanceImpl.trigger(NodeInstanceImpl.java:225)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091414361Z         at org.jbpm.workflow.instance.impl.NodeInstanceImpl.triggerNodeInstance(NodeInstanceImpl.java:424)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091419461Z         at org.jbpm.workflow.instance.impl.NodeInstanceImpl.triggerNodeInstance(NodeInstanceImpl.java:409)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091425251Z         at org.jbpm.workflow.instance.impl.NodeInstanceImpl.triggerCompleted(NodeInstanceImpl.java:379)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091431011Z         at org.jbpm.workflow.instance.impl.ExtendedNodeInstanceImpl.triggerCompleted(ExtendedNodeInstanceImpl.java:51)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091436881Z         at org.jbpm.workflow.instance.node.EventNodeInstance.triggerCompleted(EventNodeInstance.java:151)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091517823Z         at org.jbpm.workflow.instance.node.EventNodeInstance.signalEvent(EventNodeInstance.java:69)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091562534Z         at org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl.signalEvent(WorkflowProcessInstanceImpl.java:643)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091580144Z         at org.kie.kogito.process.impl.AbstractProcessInstance.send(AbstractProcessInstance.java:324)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091587124Z         at org.kie.kogito.process.impl.ProcessServiceImpl.lambda$signalProcessInstance$43(ProcessServiceImpl.java:375)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091593144Z         at java.base/java.util.Optional.map(Optional.java:265)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091601154Z         at org.kie.kogito.process.impl.ProcessServiceImpl.lambda$signalProcessInstance$44(ProcessServiceImpl.java:374)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091607054Z         at org.kie.kogito.services.uow.UnitOfWorkExecutor.executeInUnitOfWork(UnitOfWorkExecutor.java:37)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091612675Z         at org.kie.kogito.process.impl.ProcessServiceImpl.signalProcessInstance(ProcessServiceImpl.java:372)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091617845Z         at org.kie.kogito.event.impl.ProcessEventDispatcher.signalProcessInstance(ProcessEventDispatcher.java:125)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091621625Z         at org.kie.kogito.event.impl.ProcessEventDispatcher.lambda$handleMessageWithReference$3(ProcessEventDispatcher.java:115)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091625325Z         at java.base/java.util.Optional.map(Optional.java:265)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091628875Z         at org.kie.kogito.event.impl.ProcessEventDispatcher.handleMessageWithReference(ProcessEventDispatcher.java:114)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091632465Z         at org.kie.kogito.event.impl.ProcessEventDispatcher.lambda$dispatch$0(ProcessEventDispatcher.java:72)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091636055Z         at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1700)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091639585Z         at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091643145Z         at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.091646655Z         at java.base/java.lang.Thread.run(Thread.java:829)
erstwhile-wolf-genny-prd-lojing-79bc7bf67f-269qw 2023-01-23T04:47:02.092138054Z Caused by: java.lang.NullPointerException
 */