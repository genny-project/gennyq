package life.genny.qwandaq.serialization.baseentity;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.infinispan.protostream.MessageMarshaller;

import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.entity.BaseEntity;

public class BaseEntityMessageMarshaller implements MessageMarshaller<BaseEntity> {

	@Override
	public Class<BaseEntity> getJavaClass() {
		return BaseEntity.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.entity.BaseEntity";
	}

	// @Override
	public BaseEntity readFrom(ProtoStreamReader reader) throws IOException {
		BaseEntity be = new BaseEntity();
		be.setId(reader.readLong("id"));
		be.setCode(reader.readString("code"));
		Long createdLong = reader.readLong("created");
		if (createdLong != null) {
			be.setCreated(LocalDateTime.ofEpochSecond(createdLong / 1000, 0, ZoneOffset.UTC));
		}
		be.setName(reader.readString("name"));
		be.setRealm(reader.readString("realm"));
		Integer statusInt = reader.readInt("status");
		be.setStatus(EEntityStatus.valueOf(reader.readInt("status")));
		Long updatedLong = reader.readLong("updated");
		if (updatedLong != null) {
			be.setUpdated(LocalDateTime.ofEpochSecond(updatedLong / 1000, 0, ZoneOffset.UTC));
		}
		return be;
	}

	// @Override
	public void writeTo(ProtoStreamWriter writer, BaseEntity be) throws IOException {
		// writer.writeLong("id", be.getId());
		writer.writeString("code", be.getCode());
		LocalDateTime created = be.getCreated();
		Long createdLong = created != null ? created.toEpochSecond(ZoneOffset.UTC)*1000 : null;
		writer.writeLong("created", createdLong);
		writer.writeString("name", be.getName());
		writer.writeString("realm", be.getRealm());
		writer.writeInt("status", be.getStatus().ordinal());
		LocalDateTime updated = be.getUpdated();
		Long updatedLong = created != null ? updated.toEpochSecond(ZoneOffset.UTC)*1000 : null;
		writer.writeLong("updated", updatedLong);
	}

}
