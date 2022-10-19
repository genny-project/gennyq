//package life.genny.gadaq.utils;
//
//import org.jboss.logging.Logger;
//
//import javax.interceptor.AroundInvoke;
//import javax.interceptor.Interceptor;
//import javax.interceptor.InterceptorBinding;
//import javax.interceptor.InvocationContext;
//import java.lang.invoke.MethodHandles;
//import java.util.concurrent.atomic.AtomicReference;
//
//
//@Interceptor
//public class JtiInterceptor {
//    static final AtomicReference<Object> LOG = new AtomicReference<Object>();
//
//    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
//
//    @AroundInvoke
//    Object log(InvocationContext ctx) throws Exception {
//        Object ret = ctx.proceed();
//        LOG.set(ret);
//        log.info("==========================JtiInterceptor==========================");
//
//        return ret;
//    }
//
//}
