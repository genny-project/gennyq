package life.genny.qwandaq.loadtesting;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.entity.search.SearchEntity;

@ApplicationScoped
public class LoadTestJobs {

	private static final Logger log = Logger.getLogger(LoadTestJobs.class);

	public final Jsonb jsonb = JsonbBuilder.create();

	static Map<String, TestJob> jobs = new ConcurrentHashMap<String, TestJob>();
	
	/** 
	 * @return Map&lt;String, TestJob&gt;
	 */
	public Map<String, TestJob> getJobs() {
		return jobs;
	}
	
	/**
	 * Retrieve a {@link TestJob} from the job cache.
	 * Expecting Base Entity code + "_" + {@link TestJob#uuid}
	 *
	 * @param uniqueCode the uniqueCode to set
	 * @return TestJob
	 */
	public TestJob getJob(String uniqueCode) {
		return jobs.get(uniqueCode);
	}

	/**
	 * Put a new {@link TestJob} in the jobs store with a {@link SearchEntity} code as the key
	 *
	 * @param entity {@link SearchEntity} pertaining to the {@link TestJob}
	 * @param job {@link TestJob} to add
	 */
	void putJob(SearchEntity entity, TestJob job) {
		jobs.put(entity.getCode(), job);
	}
}
