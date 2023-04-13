package life.genny.qwandaq.serialization.question;

import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.converter.CapabilityConverter;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class QuestionMessageMarshaller implements MessageMarshaller<Question> {

	public static final String TYPE_NAME = "life.genny.qwandaq.persistence.question.Question";

	@Override
	public Class<Question> getJavaClass() {
		return Question.class;
	}

	@Override
	public String getTypeName() {
		return TYPE_NAME;
	}

	// @Override
	public Question readFrom(ProtoStreamReader reader) throws IOException {
		Question question = new Question();
		question.setId(reader.readLong("id"));
		Long createdLong = reader.readLong("created");
		if (createdLong != null) {
			question.setCreated(LocalDateTime.ofEpochSecond(createdLong / 1000, 0, ZoneOffset.UTC));
		}
		question.setName(reader.readString("name"));
		question.setRealm(reader.readString("realm"));
		Long updatedLong = reader.readLong("updated");
		if (updatedLong != null) {
			question.setUpdated(LocalDateTime.ofEpochSecond(updatedLong / 1000, 0, ZoneOffset.UTC));
		}
		question.setCode(reader.readString("code"));
		Integer statusInt = reader.readInt("status");
		question.setStatus(EEntityStatus.valueOf(statusInt));
		question.setAttributeCode(reader.readString("attributeCode"));
		question.setDirections(reader.readString("directions"));
		question.setHelper(reader.readString("helper"));
		question.setHtml(reader.readString("html"));
		question.setIcon(reader.readString("icon"));
		question.setMandatory(reader.readBoolean("mandatory"));
		question.setOneshot(reader.readBoolean("oneshot"));
		question.setPlaceholder(reader.readString("placeholder"));
		question.setReadonly(reader.readBoolean("readonly"));
		question.setAttributeId(reader.readLong("attribute_id"));
		question.setCapabilityRequirements(CapabilityConverter.convertToEA(reader.readString("capreqs")));
		return question;
	}

	// @Override
	public void writeTo(ProtoStreamWriter writer, Question question) throws IOException {
		writer.writeLong("id", question.getId());
		LocalDateTime created = question.getCreated();
		Long createdLong = created != null ? created.toEpochSecond(ZoneOffset.UTC)*1000 : null;
		writer.writeLong("created", createdLong);
		writer.writeString("name", question.getName());
		writer.writeString("realm", question.getRealm());
		LocalDateTime updated = question.getUpdated();
		Long updatedLong = created != null ? updated.toEpochSecond(ZoneOffset.UTC)*1000 : null;
		writer.writeLong("updated", updatedLong);
		writer.writeString("code", question.getCode());
		writer.writeInt("status", question.getStatus().ordinal());
		writer.writeString("attributeCode", question.getAttributeCode());
		writer.writeString("directions", question.getDirections());
		writer.writeString("helper", question.getHelper());
		writer.writeString("html", question.getHtml());
		writer.writeString("icon", question.getIcon());
		writer.writeBoolean("mandatory", question.getMandatory());
		writer.writeBoolean("oneshot", question.getOneshot());
		writer.writeString("placeholder", question.getPlaceholder());
		writer.writeBoolean("readonly", question.getReadonly());
		writer.writeLong("attribute_id", question.getAttributeId());
		writer.writeString("capreqs", CapabilityConverter.convertToDBColumn(question.getCapabilityRequirements()));
	}

}
