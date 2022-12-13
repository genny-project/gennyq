package life.genny.test.kogito;

import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.junit.Test;

import life.genny.kogito.common.core.Dispatch;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.PCM;

public class DispatchTest {
	
	@Inject
	Logger log;

	@Inject
	Dispatch dispatch;

	@Test
	public void traversalTest() {

		BaseEntity source = new BaseEntity("PER_SOURCE", "Source");
		BaseEntity target = new BaseEntity("PER_TARGET", "Target");

		// PCM pcm = new PCM("PCM_TEST", "Test PCM");
		// pcm.setTargetCode("[[T]]");

		// dispatch.traversePCM(pcm, source, target, msg, processData);
	}
}
