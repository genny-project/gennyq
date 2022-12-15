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
		Long lastActiveLong = reader.readLong("last_active");
		if (lastActiveLong != null) {
			userStore.setLastActive(LocalDateTime.ofEpochSecond(lastActiveLong / 1000, 0, ZoneOffset.UTC));
		}
		return userStore;
	}

	// @Override
	public void writeTo(ProtoStreamWriter writer, UserStore userStore) throws IOException {
		writer.writeString("realm", userStore.getRealm());
		writer.writeString("usercode", userStore.getUsercode());
		writer.writeString("jti_access", userStore.getJtiAccess());
		LocalDateTime lastActive = userStore.getLastActive();
		Long lastActiveLong = lastActive != null ? lastActive.toEpochSecond(ZoneOffset.UTC)*1000 : null;
		writer.writeLong("last_active", lastActiveLong);
	}

}
