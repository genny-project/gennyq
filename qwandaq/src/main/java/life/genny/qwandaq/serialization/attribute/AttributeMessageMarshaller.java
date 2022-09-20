package life.genny.qwandaq.serialization.attribute;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.infinispan.protostream.MessageMarshaller;

public class AttributeMessageMarshaller implements MessageMarshaller<Attribute> {

	@Override
	public Class<Attribute> getJavaClass() {
		return Attribute.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.serialization.attribute.Attribute";
	}

	// @Override
	public Attribute readFrom(ProtoStreamReader reader) throws IOException {
		Attribute att = new Attribute();
		att.setId(reader.readLong("id"));
		att.setCode(reader.readString("code"));
		Long createdLong = reader.readLong("created");
		if (createdLong != null) {
			att.setCreated(LocalDateTime.ofEpochSecond(createdLong / 1000, 0, ZoneOffset.UTC));
		}
		att.setName(reader.readString("name"));
		att.setRealm(reader.readString("realm"));

		Long updatedLong = reader.readLong("updated");
		if (updatedLong != null) {
			att.setUpdated(LocalDateTime.ofEpochSecond(updatedLong / 1000, 0, ZoneOffset.UTC));
		}
		att.setClassName(reader.readString("className"));
		att.setDttCode(reader.readString("dttCode"));
		att.setInputMask(reader.readString("inputmask"));
		att.setTypeName(reader.readString("typeName"));
		att.setValidationList(reader.readString("validation_list"));
		att.setDefaultPrivacyFlag(reader.readBoolean("defaultPrivacyFlag"));
		att.setDefaultValue(reader.readString("defaultValue"));
		att.setDescription(reader.readString("description"));
		att.setHelp(reader.readString("help"));
		att.setPlaceholder(reader.readString("placeholder"));
		att.setComponent(reader.readString("component"));
		att.setIcon(reader.readString("icon"));
		att.setStatus(reader.readInt("status"));
		return att;
	}

	// @Override
	public void writeTo(ProtoStreamWriter writer, Attribute att) throws IOException {
		// writer.writeLong("id", be.getId());
		writer.writeString("code", att.getCode());
		LocalDateTime created = att.getCreated();
		Long createdLong = created != null ? created.toEpochSecond(ZoneOffset.UTC) * 1000 : null;
		writer.writeLong("created", createdLong);
		writer.writeString("name", att.getName());
		writer.writeString("realm", att.getRealm());
		LocalDateTime updated = att.getUpdated();
		Long updatedLong = created != null ? updated.toEpochSecond(ZoneOffset.UTC) * 1000 : null;
		writer.writeLong("updated", updatedLong);
		writer.writeString("className", att.getClassName());
		writer.writeString("dttCode", att.getDttCode());
		writer.writeString("inputmask", att.getInputMask());
		writer.writeString("typeName", att.getTypeName());
		writer.writeString("validation_list", att.getValidationList());
		writer.writeBoolean("defaultPrivacyFlag", att.getDefaultPrivacyFlag());
		writer.writeString("defaultValue", att.getDefaultValue());
		writer.writeString("description", att.getDescription());
		writer.writeString("help", att.getHelp());
		writer.writeString("placeholder", att.getPlaceholder());
		writer.writeString("component", att.getComponent());
		writer.writeString("icon", att.getIcon());
		writer.writeInt("status", att.getStatus());
	}

}
