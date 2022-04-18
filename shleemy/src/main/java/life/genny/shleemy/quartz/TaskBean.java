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

		// setup message for persistance
		scheduleMessage.id = null;
		scheduleMessage.setRealm(userToken.getRealm());
		scheduleMessage.setSourceCode(userToken.getUserCode());
		scheduleMessage.setToken(userToken.getToken());

		// grab fields of message
		String realm = scheduleMessage.getRealm();
		String code = scheduleMessage.getCode();
		String sourceCode = scheduleMessage.getSourceCode();
		String channel = scheduleMessage.getChannel();

		String cron = scheduleMessage.getCron();
		LocalDateTime trigger = scheduleMessage.getTrigger();
		String messageJson = scheduleMessage.getJsonMessage();

		log.info("Persisting new Schedule -> " + code + ":"
				+ (trigger != null ? trigger : cron) + " from " + sourceCode);

		scheduleMessage.persist();

		// create job from job map
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("sourceCode", userToken.getUserCode());
		jobDataMap.put("token", userToken.getToken());
		jobDataMap.put("channel", channel);
		jobDataMap.put("code", code);
		jobDataMap.put("message", messageJson);

		JobDetail job = JobBuilder.newJob(ShleemyJob.class)
			.withIdentity(code, realm)
			.setJobData(jobDataMap)
			.build();

		Trigger jobTrigger = null;
		String scheduledFor = null;

		// handle cron trigger
		if (!cron.isBlank()) {

			CronScheduleBuilder cronSchedule = CronScheduleBuilder.cronSchedule(cron);
			jobTrigger = TriggerBuilder.newTrigger()
				.withIdentity(code, realm)
				.startNow()
				.withSchedule(cronSchedule)
				.build();

			scheduledFor = cron;

		// handle time trigger
		} else if (trigger != null) {

			Date scheduledDateTime = Date.from(trigger.atZone(ZoneId.systemDefault()).toInstant());
			jobTrigger = TriggerBuilder.newTrigger()
				.withIdentity(code, realm)
				.startAt(scheduledDateTime)
				.forJob(code, realm)
				.build();

			scheduledFor = scheduledDateTime.toString();
		}

		log.info("Scheduling " + userToken.getUserCode() + ":" + code + ":" + userToken.getEmail() + " for Realm: " + realm
				+ ", Trigger: " + scheduledFor + ", Current time: " + LocalDateTime.now());

		// schedule the job
		quartz.scheduleJob(job, jobTrigger);
	}

	public void abortSchedule(String code, GennyToken userToken) throws SchedulerException {

		JobKey jobKey = new JobKey(code, userToken.getRealm());
		quartz.deleteJob(jobKey);
	}

	@Transactional
	void performTask(JobExecutionContext context) {

		log.info("Executing Task: " + context.getFireTime());

		// grab fields of scheduled message
		String sourceCode = context.getJobDetail().getJobDataMap().getString("sourceCode");
		String channel = context.getJobDetail().getJobDataMap().getString("channel");
		String code = context.getJobDetail().getJobDataMap().getString("code");
		String token = context.getJobDetail().getJobDataMap().getString("token");
		GennyToken userToken = new GennyToken(token);

		String jsonMessage = (String) context.getJobDetail().getJobDataMap().get("message");

		log.info("Sending " + sourceCode + ":" + code + ":" + userToken.getEmail() + " to " + channel + " for " 
				+ userToken.getRealm() + " at " + LocalDateTime.now() + ", jsonMessage:" + jsonMessage);

		// send to channel specified by schedule message
		KafkaUtils.writeMsg(channel, jsonMessage);
	}

}
