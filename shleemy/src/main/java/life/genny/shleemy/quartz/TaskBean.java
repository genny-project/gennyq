package life.genny.shleemy.quartz;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

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

	private static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	org.quartz.Scheduler quartz;

	@Inject
	UserToken userToken;

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

	@Transactional
	public void addSchedule(QScheduleMessage scheduleMessage) throws SchedulerException {

		log.info(jsonb.toJson(scheduleMessage));

		// setup message for persistance
		scheduleMessage.id = null;
		scheduleMessage.setRealm(userToken.getProductCode());
		scheduleMessage.setSourceCode(userToken.getUserCode());
		scheduleMessage.setToken(userToken.getToken());

		// grab fields of message
		String productCode = scheduleMessage.getProductCode();
		String code = scheduleMessage.getCode();
		String sourceCode = scheduleMessage.getSourceCode();
		String channel = scheduleMessage.getChannel();

		String cron = scheduleMessage.getCron();
		LocalDateTime triggerTime = scheduleMessage.getTriggerTime();
		String messageJson = scheduleMessage.getJsonMessage();

		log.info("Persisting new Schedule -> " + code + ":"
				+ (triggerTime != null ? triggerTime : cron) + " from " + sourceCode);

		scheduleMessage.persist();

		// create job from job map
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("sourceCode", userToken.getUserCode());
		jobDataMap.put("token", userToken.getToken());
		jobDataMap.put("channel", channel);
		jobDataMap.put("code", code);
		jobDataMap.put("message", messageJson);

		JobDetail job = JobBuilder.newJob(ShleemyJob.class)
			.withIdentity(code, productCode)
			.setJobData(jobDataMap)
			.build();

		Trigger jobTrigger = null;
		String scheduledFor = null;

		// handle cron trigger
		if (!StringUtils.isBlank(cron)) {

			CronScheduleBuilder cronSchedule = CronScheduleBuilder.cronSchedule(cron);
			jobTrigger = TriggerBuilder.newTrigger()
				.withIdentity(code, productCode)
				.startNow()
				.withSchedule(cronSchedule)
				.build();

			scheduledFor = cron;

		// handle time trigger
		} else if (triggerTime != null) {

			Date scheduledDateTime = Date.from(triggerTime.atZone(ZoneId.systemDefault()).toInstant());
			jobTrigger = TriggerBuilder.newTrigger()
				.withIdentity(code, productCode)
				.startAt(scheduledDateTime)
				.forJob(code, productCode)
				.build();

			scheduledFor = scheduledDateTime.toString();
		} else {
			log.error("No valid triggerTime or cron was provided!");
		}

		log.info("Scheduling " + userToken.getUserCode() + ":" + code + ":" + userToken.getEmail() + " for Realm: " + productCode
				+ ", Trigger: " + scheduledFor + ", Current time: " + LocalDateTime.now());

		// schedule the job
		quartz.scheduleJob(job, jobTrigger);
	}

	public void abortSchedule(String code) throws SchedulerException {

		JobKey jobKey = new JobKey(code, userToken.getProductCode());
		quartz.deleteJob(jobKey);
	}

	void performTask(JobExecutionContext context) {

		log.info("Executing Task: " + context.getFireTime());

		// grab fields of scheduled message
		String sourceCode = context.getJobDetail().getJobDataMap().getString("sourceCode");
		String channel = context.getJobDetail().getJobDataMap().getString("channel");
		String code = context.getJobDetail().getJobDataMap().getString("code");
		String token = context.getJobDetail().getJobDataMap().getString("token");
		GennyToken gennyToken = new GennyToken(token);

		String jsonMessage = (String) context.getJobDetail().getJobDataMap().get("message");

		log.info("Sending " + sourceCode + ":" + code + ":" + gennyToken.getEmail() + " to " + channel + " for " 
				+ gennyToken.getProductCode() + " at " + LocalDateTime.now() + ", jsonMessage:" + jsonMessage);

		// send to channel specified by schedule message
		KafkaUtils.writeMsg(channel, jsonMessage);
	}

}
