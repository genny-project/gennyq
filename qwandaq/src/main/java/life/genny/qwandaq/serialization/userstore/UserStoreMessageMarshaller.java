package life.genny.qwandaq.serialization.userstore;

import life.genny.qwandaq.EEntityStatus;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class UserStoreMessageMarshaller implements MessageMarshaller<UserStore> {

	@Override
	public Class<UserStore> getJavaClass() {
		return UserStore.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.persistence.userstore.UserStore";
	}

	// @Override
	public UserStore readFrom(ProtoStreamReader reader) throws IOException {
		UserStore userStore = new UserStore();
		userStore.setRealm(reader.readString("realm"));
		userStore.setUsercode(reader.readString("usercode"));
		userStore.setJtiAccess(reader.readString("jti_access"));
		userStore.setLastActive(reader.readLong("last_active"));
		return userStore;
	}

	// @Override
	public void writeTo(ProtoStreamWriter writer, UserStore userStore) throws IOException {
		writer.writeString("realm", userStore.getRealm());
		writer.writeString("usercode", userStore.getUsercode());
		writer.writeString("jti_access", userStore.getJtiAccess());
		writer.writeLong("last_active", userStore.getLastActive());
	}

}
