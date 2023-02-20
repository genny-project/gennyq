package life.genny.qwandaq.serialization.validation;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import life.genny.qwandaq.converter.CapabilityConverter;
import life.genny.qwandaq.converter.ValidationListConverter;
import org.infinispan.protostream.MessageMarshaller;

import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.validation.Validation;

public class ValidationMessageMarshaller implements MessageMarshaller<Validation> {

	@Override
	public Class<Validation> getJavaClass() {
		return Validation.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.persistence.validation.Validation";
	}

	// @Override
	public Validation readFrom(ProtoStreamReader reader) throws IOException {
		Validation validation = new Validation();
		validation.setId(reader.readLong("id"));
		validation.setCode(reader.readString("code"));
		Long createdLong = reader.readLong("created");
		if (createdLong != null) {
			validation.setCreated(LocalDateTime.ofEpochSecond(createdLong / 1000, 0, ZoneOffset.UTC));
		}
		validation.setName(reader.readString("name"));
		validation.setRealm(reader.readString("realm"));
		Integer statusInt = reader.readInt("status");
		validation.setStatus(EEntityStatus.valueOf(statusInt));
		Long updatedLong = reader.readLong("updated");
		if (updatedLong != null) {
			validation.setUpdated(LocalDateTime.ofEpochSecond(updatedLong / 1000, 0, ZoneOffset.UTC));
		}
		validation.setErrormsg(reader.readString("errormsg"));
		validation.setMultiAllowed(reader.readBoolean("multiAllowed"));
		validation.setOptions(reader.readString("options"));
		validation.setRecursiveGroup(reader.readBoolean("recursiveGroup"));
		validation.setRegex(reader.readString("regex"));
		ValidationListConverter validationListConverter = new ValidationListConverter();
		validation.setSelectionBaseEntityGroupList(validationListConverter.convertFromStringToStringList(reader.readString("selection_grp")));
		return validation;
	}

	// @Override
	public void writeTo(ProtoStreamWriter writer, Validation validation) throws IOException {
		// writer.writeLong("id", validation.getId());
		writer.writeString("code", validation.getCode());
		LocalDateTime created = validation.getCreated();
		Long createdLong = created != null ? created.toEpochSecond(ZoneOffset.UTC)*1000 : null;
		writer.writeLong("created", createdLong);
		writer.writeString("name", validation.getName());
		writer.writeString("realm", validation.getRealm());
		writer.writeInt("status", validation.getStatus().ordinal());
		LocalDateTime updated = validation.getUpdated();
		Long updatedLong = created != null ? updated.toEpochSecond(ZoneOffset.UTC)*1000 : null;
		writer.writeLong("updated", updatedLong);
		writer.writeString("errormsg", validation.getErrormsg());
		writer.writeBoolean("multiAllowed", validation.getMultiAllowed());
		writer.writeString("options", validation.getOptions());
		writer.writeBoolean("recursiveGroup", validation.getRecursiveGroup());
		writer.writeString("regex", validation.getRegex());
		ValidationListConverter validationListConverter = new ValidationListConverter();
		writer.writeString("selection_grp", validationListConverter.convertToString(validation.getSelectionBaseEntityGroupList()));
	}

}
