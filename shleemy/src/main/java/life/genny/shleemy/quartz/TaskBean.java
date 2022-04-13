package life.genny.shleemy.quartz;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import io.quarkus.runtime.StartupEvent;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.quartz.*;

import life.genny.shleemy.endpoints.ScheduleResource;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.message.QScheduleMessage;
import life.genny.shleemy.live.data.InternalProducer;
import life.genny.serviceq.Service;

@ApplicationScoped
public class TaskBean {

    private static final Logger log = Logger.getLogger(ScheduleResource.class);

    @Inject
    org.quartz.Scheduler quartz;

    @Inject
    InternalProducer producer;

	@Inject
	Service service;

    void onStart(@Observes StartupEvent event) {

		service.showConfiguration();

		service.initToken();
		service.initDatabase();
		service.initKafka();
		log.info("[*] Finished Startup!");
    }

    public void abortSchedule(String uniqueCode, GennyToken userToken) throws SchedulerException {

        JobKey jobKey = new JobKey(uniqueCode, userToken.getRealm());
        quartz.deleteJob(jobKey);
    }

    public String addSchedule(QScheduleMessage scheduleMessage, GennyToken userToken) throws SchedulerException {

        scheduleMessage.token = userToken.getToken();

        String messageJson = scheduleMessage.jsonMessage;

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("message", messageJson);
        jobDataMap.put("sourceCode", userToken.getUserCode());
        jobDataMap.put("token", userToken.getToken());
        jobDataMap.put("channel", scheduleMessage.channel);
        jobDataMap.put("code", scheduleMessage.code);

        String uniqueCode = scheduleMessage.code;

        JobDetail job = JobBuilder.newJob(MyJob.class).withIdentity(uniqueCode, userToken.getRealm())
                .setJobData(jobDataMap).build();

        Trigger trigger = null;
        log.info("scheduleMessage: " + scheduleMessage);

		// handle cron trigger
        if (!scheduleMessage.cron.isBlank()) {

			CronScheduleBuilder cronSchedule = CronScheduleBuilder.cronSchedule(scheduleMessage.cron);
            trigger = TriggerBuilder.newTrigger()
					.withIdentity(uniqueCode, userToken.getRealm())
					.startNow()
                    .withSchedule(cronSchedule)
                    .build();

            log.info("Scheduled " + userToken.getUserCode() + ":" + userToken.getEmail() + " for " + userToken.getRealm()
                            + " for trigger at " + scheduleMessage.cron + " and now is " + LocalDateTime.now());

		// handle time trigger
        } else if (scheduleMessage.triggertime != null) {

            Date scheduledDateTime = Date.from(scheduleMessage.triggertime.atZone(ZoneId.systemDefault()).toInstant());
            trigger = TriggerBuilder.newTrigger()
					.withIdentity(uniqueCode, userToken.getRealm())
                    .startAt(scheduledDateTime)
                    .forJob(uniqueCode, userToken.getRealm())
                    .build();

            log.info("Scheduled " + userToken.getUserCode() + ":" + uniqueCode + ":" + userToken.getEmail() + " for " + userToken.getRealm()
                            + " for trigger at " + scheduledDateTime + " and now is " + LocalDateTime.now());
        }

        quartz.scheduleJob(job, trigger);

        return uniqueCode;
    }

    @Transactional
    void performTask(JobExecutionContext context) {

        log.info("Executing Scheduler Task: " + context.getFireTime());
        String bridgeUrl = ConfigProvider.getConfig().getValue("bridge.service.url", String.class);

        String sourceCode = context.getJobDetail().getJobDataMap().getString("sourceCode");
        String channel = context.getJobDetail().getJobDataMap().getString("channel");
        String code = context.getJobDetail().getJobDataMap().getString("code");
        String token = context.getJobDetail().getJobDataMap().getString("token");
        GennyToken userToken = new GennyToken(token);

        String scheduleMsgJson = (String) context.getJobDetail().getJobDataMap().get("message");

        log.info(scheduleMsgJson);

        producer.getToEvents().send(scheduleMsgJson);

        log.info("Executing Schedule " + sourceCode + ":" + code + ":" + userToken.getEmail() + " for " + userToken.getRealm()
                + " at " + LocalDateTime.now() + " sending through bridgeUrl=" + bridgeUrl + ", scheduleMsgJson:" + scheduleMsgJson);
    }

    // A new instance of MyJob is created by Quartz for every job execution
    public static class MyJob implements Job {

        @Inject
        TaskBean taskBean;

        public void execute(JobExecutionContext context) throws JobExecutionException {
            taskBean.performTask(context);
        }

    }
}
