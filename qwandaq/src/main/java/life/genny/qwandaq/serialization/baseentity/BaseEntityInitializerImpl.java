package life.genny.qwandaq.serialization.baseentity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;

//import org.apache.logging.log4j.Logger;
//import org.apache.logging.log4j.LogManager;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.impl.ResourceUtils;

public class BaseEntityInitializerImpl implements SerializationContextInitializer {

	//private static final Logger log = LogManager.getLogger(BaseEntityInitializerImpl.class);

	@Override
	public String getProtoFileName() {
		return "/life/genny/qwandaq/serialization/protos/baseentity.proto";
	}

	@Override
	public String getProtoFile() throws UncheckedIOException {
		//System.out.println("\n\n\n\n getProtoFileName() -> "+getProtoFileName()+" \n\n\n\n");
		return ResourceUtils.getResourceAsString(getClass(), getProtoFileName());
		/*try {
			return new String(Files.readAllBytes(Paths.get(getProtoFileName())));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}*/
	}

	@Override
	public void registerSchema(SerializationContext serCtx) {
		serCtx.registerProtoFiles(FileDescriptorSource.fromString(getProtoFileName(), getProtoFile()));

	}

	@Override
	public void registerMarshallers(SerializationContext serCtx) {
		serCtx.registerMarshaller(new BaseEntityMessageMarshaller());
	}

}
