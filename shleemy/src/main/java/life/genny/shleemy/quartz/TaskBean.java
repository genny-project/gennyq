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
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.serviceq.Service;

@ApplicationScoped
public class TaskBean {

	private static final Logger log = Logger.getLogger(TaskBean.class);

	@Inject
	org.quartz.Scheduler quartz;

	/**
	 * A new instance of ShleemyJob is created by Quartz for every job execution
	 */
	public static class ShleemyJob implements Job {

		@Inject
		TaskBean taskBean;

		public void execute(JobExecutionContext context) throws JobExecutionException {
			taskBean.performTask(context);
		}
	}

	public void addSchedule(QScheduleMessage scheduleMessage, GennyToken userToken) throws SchedulerException {

		scheduleMessage.id = null;
		scheduleMessage.realm = userToken.getRealm();
		scheduleMessage.sourceCode = userToken.getUserCode();
		scheduleMessage.token = userToken.getToken();

		log.info("Persisting new Schedule -> " + scheduleMessage.code + ":" 
				+ scheduleMessage.triggertime + " from " + scheduleMessage.sourceCode);

		scheduleMessage.persist();

		String messageJson = scheduleMessage.jsonMessage;

		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("message", messageJson);
		jobDataMap.put("sourceCode", userToken.getUserCode());
		jobDataMap.put("token", userToken.getToken());
		jobDataMap.put("channel", scheduleMessage.channel);
		jobDataMap.put("code", scheduleMessage.code);

		JobDetail job = JobBuilder.newJob(ShleemyJob.class)
			.withIdentity(scheduleMessage.code, userToken.getRealm())
			.setJobData(jobDataMap)
			.build();

		Trigger trigger = null;
		String scheduledFor = null;

		// handle cron trigger
		if (!scheduleMessage.cron.isBlank()) {

			CronScheduleBuilder cronSchedule = CronScheduleBuilder.cronSchedule(scheduleMessage.cron);
			trigger = TriggerBuilder.newTrigger()
				.withIdentity(scheduleMessage.code, userToken.getRealm())
				.startNow()
				.withSchedule(cronSchedule)
				.build();

			scheduledFor = scheduleMessage.cron;

			// handle time trigger
		} else if (scheduleMessage.triggertime != null) {

			Date scheduledDateTime = Date.from(scheduleMessage.triggertime.atZone(ZoneId.systemDefault()).toInstant());
			trigger = TriggerBuilder.newTrigger()
				.withIdentity(scheduleMessage.code, userToken.getRealm())
				.startAt(scheduledDateTime)
				.forJob(scheduleMessage.code, userToken.getRealm())
				.build();

			scheduledFor = scheduledDateTime.toString();
		}

		log.info("Scheduled " + userToken.getUserCode() + ":" + scheduleMessage.code + ":" + userToken.getEmail() + " for " + userToken.getRealm()
				+ ", Trigger: " + scheduledFor + ", Current time: " + LocalDateTime.now());

		quartz.scheduleJob(job, trigger);
	}

	public void abortSchedule(String code, GennyToken userToken) throws SchedulerException {

		JobKey jobKey = new JobKey(code, userToken.getRealm());
		quartz.deleteJob(jobKey);
	}

	@Transactional
	void performTask(JobExecutionContext context) {

		log.info("Executing Task: " + context.getFireTime());

		String sourceCode = context.getJobDetail().getJobDataMap().getString("sourceCode");
		String channel = context.getJobDetail().getJobDataMap().getString("channel");
		String code = context.getJobDetail().getJobDataMap().getString("code");
		String token = context.getJobDetail().getJobDataMap().getString("token");
		GennyToken userToken = new GennyToken(token);

		String scheduleMsgJson = (String) context.getJobDetail().getJobDataMap().get("message");

		log.info("Sending event " + sourceCode + ":" + code + ":" + userToken.getEmail() + " for " 
				+ userToken.getRealm() + " at " + LocalDateTime.now() + ", scheduleMsgJson:" + scheduleMsgJson);

		KafkaUtils.writeMsg("events", scheduleMsgJson);
	}

}
