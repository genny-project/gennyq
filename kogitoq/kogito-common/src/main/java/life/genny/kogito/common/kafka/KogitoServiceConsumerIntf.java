package life.genny.kogito.common.kafka;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public interface KogitoServiceConsumerIntf {

	public static String SRC_PATH = "/deployments/kogito-protos";
	public static String DEST_PATH = "/deployments/protobuf/";

	public void getData(String data);

	public void getEvent(String event);

	/**
	 * Copy the service protos into the shared volume space for data-index.
	 */
	public static void initialiseProtobufs() {
		for (File file : new File(SRC_PATH).listFiles()) {
			try {
				Files.copy(file.toPath(), new File(DEST_PATH + file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
