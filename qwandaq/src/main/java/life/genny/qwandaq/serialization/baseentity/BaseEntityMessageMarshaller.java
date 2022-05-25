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
		be.setCreated(LocalDateTime.ofEpochSecond(reader.readLong("created")/1000, 0, ZoneOffset.UTC));
		be.setName(reader.readString("name"));
		be.setRealm(reader.readString("realm"));
		be.setStatus(EEntityStatus.valueOf(""+reader.readInt("status")));
		be.setUpdated(LocalDateTime.ofEpochSecond(reader.readLong("updated")/1000, 0, ZoneOffset.UTC));
		return be;
	}

	// @Override
	public void writeTo(ProtoStreamWriter writer, BaseEntity be) throws IOException {
		writer.writeLong("id", be.getId());
		writer.writeString("code", be.getCode());
		writer.writeLong("created", be.getCreated().toEpochSecond(ZoneOffset.UTC)*1000);
		writer.writeString("name", be.getName());
		writer.writeString("realm", be.getRealm());
		writer.writeInt("status", be.getStatus().ordinal());
		writer.writeLong("updated", be.getUpdated().toEpochSecond(ZoneOffset.UTC)*1000);
	}

}
