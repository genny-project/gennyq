package life.genny.qwandaq.serialization.baseentity;

import java.io.UncheckedIOException;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.impl.ResourceUtils;

public class BaseEntityInitializerImpl implements SerializationContextInitializer {

	@Override
	public String getProtoFileName() {
		// return "/life/genny/qwandaq/serialization/protos/baseentity.proto";
		return "baseentity.proto";
	}

	@Override
	public String getProtoFile() throws UncheckedIOException {
		return ResourceUtils.getResourceAsString(getClass(), "/life/genny/qwandaq/serialization/protos/" + getProtoFileName());
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
