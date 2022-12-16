package life.genny.qwandaq.serialization.userstore;

import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class UserStoreKeyMessageMarshaller implements MessageMarshaller<UserStoreKey> {

	@Override
	public Class<UserStoreKey> getJavaClass() {
		return UserStoreKey.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.serialization.userstore.UserStoreKey";
	}

	@Override
	public UserStoreKey readFrom(ProtoStreamReader reader) throws IOException {
		String productCode = reader.readString("realm");
		String usercode = reader.readString("usercode");
		UserStoreKey usk = new UserStoreKey(productCode, usercode);
		return usk;
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, UserStoreKey usk) throws IOException {
		writer.writeString("realm", usk.getRealm());
		writer.writeString("usercode", usk.getUsercode());
	}

}
