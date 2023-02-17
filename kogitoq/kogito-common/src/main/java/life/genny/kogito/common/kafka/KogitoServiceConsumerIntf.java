package life.genny.kogito.common.kafka;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.jboss.logging.Logger;

import life.genny.qwandaq.exception.runtime.LaunchException;

public interface KogitoServiceConsumerIntf {
	static Logger log = Logger.getLogger(KogitoServiceConsumerIntf.class);

	public static final String PROTOBUF_SRC = "/deployments/protobuf-location";
	public static final String PROTOBUF_DEST = "/deployments/protobuf/";

	public static final String SVG_SRC = "/deployments/svg-location";
	public static final String SVG_DEST = "/deployments/svg/";

	public static final String DEV_MODE_PROTOBUF_SRC = "target/classes/META-INF/resources/persistence/protobuf";
	public static final String DEV_MODE_SVG_SRC = "target/classes/META-INF/processSVG";

	public void getData(String data);

	public void getEvent(String event);

	/**
	 * Copy supporting files to shared spaces.
	 */
	public static void initialiseFiles() {
		initialiseProtobufs();
		initialiseSVGs();
	}

	/**
	 * Copy the service protos into the shared volume space for data-index.
	 */
	public static void initialiseProtobufs() {
		copyDirectoryContents(PROTOBUF_SRC, PROTOBUF_DEST, DEV_MODE_PROTOBUF_SRC);
	}

	/**
	 * Copy the service svg files to the shared volume space for management-console.
	 */
	public static void initialiseSVGs() {
		copyDirectoryContents(SVG_SRC, SVG_DEST, DEV_MODE_SVG_SRC);
	}

	/**
	 * Copy all files from a source directory to a destination directory.
	 *
	 * @param source The source path
	 * @param destination The destination path
	 */
	public static void copyDirectoryContents(String source, String destination, String defaultSource) {
		// select the dev mode directory if container directory not found
		File directory;
		if (Files.exists(Paths.get(source))) {
			directory = new File(source);
		} else if (Files.exists(Paths.get(defaultSource))) {
			log.info("Directory not found: " + source + ". Using Default Source: " + defaultSource + " instead.");
			directory = new File(defaultSource);
			return;
		} else {
			String msg = new StringBuilder("Could not find protobuf directory in:\n")
					.append("\t- ").append(source).append('\n')
					.append("\t- ").append(defaultSource).append('\n')
					.append("Make sure the project has been built.")
					.toString();
			throw new LaunchException(msg);
		}
		// copy all files to the correct directory
		for (File file : directory.listFiles()) {
			try {
				Files.copy(file.toPath(), new File(destination + file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
