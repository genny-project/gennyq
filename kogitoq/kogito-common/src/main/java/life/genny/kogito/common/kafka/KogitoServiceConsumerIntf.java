package life.genny.kogito.common.kafka;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import life.genny.qwandaq.exception.GennyRuntimeException;
import life.genny.qwandaq.exception.runtime.LaunchException;

public interface KogitoServiceConsumerIntf {

	public static final String SRC_PATH = "/deployments/kogito-protos";
	public static final String DEST_PATH = "/deployments/protobuf/";

	public static final String DEV_MODE_PATH = "target/classes/META-INF/resources/persistence/protobuf";

	public void getData(String data);

	public void getEvent(String event);

	/**
	 * Copy the service protos into the shared volume space for data-index.
	 */
	public static void initialiseProtobufs() {

		// select the dev mode directory if container directory not found
		File directory;
		if (Files.exists(Paths.get(SRC_PATH))) {
			directory = new File(SRC_PATH);
		} else if (Files.exists(Paths.get(DEV_MODE_PATH))) {
			// TODO: handle
			return;
		} else {
			// TODO: throw launch exception
			// throw new LaunchException("Could not find protobuf directory. Make sure the the project has been built.");
			return;
		}

		// copy all files to the correct directory
		for (File file : directory.listFiles()) {
			try {
				Files.copy(file.toPath(), new File(DEST_PATH + file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
