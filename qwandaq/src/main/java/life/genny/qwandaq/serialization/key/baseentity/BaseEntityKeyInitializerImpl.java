package life.genny.qwandaq.serialization.key.baseentity;

import java.io.UncheckedIOException;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.impl.ResourceUtils;

public class BaseEntityKeyInitializerImpl implements SerializationContextInitializer {

	@Override
	public String getProtoFileName() {
		return "/life/genny/qwandaq/serialization/protos/baseentity_key.proto";
	}

	@Override
	public String getProtoFile() throws UncheckedIOException {
		return ResourceUtils.getResourceAsString(getClass(), getProtoFileName());
	}

	@Override
	public void registerSchema(SerializationContext serCtx) {
		serCtx.registerProtoFiles(FileDescriptorSource.fromString(getProtoFileName(), getProtoFile()));

	}

	@Override
	public void registerMarshallers(SerializationContext serCtx) {
		serCtx.registerMarshaller(new BaseEntityKeyMessageMarshaller());
	}

}
