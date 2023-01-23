package life.genny.qwandaq.serialization.entityattribute;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.protostream.MessageMarshaller;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.io.IOException;
import java.time.*;

import life.genny.qwandaq.attribute.EntityAttribute;

public class EntityAttributeMessageMarshaller implements MessageMarshaller<EntityAttribute> {

	@Override
	public Class<EntityAttribute> getJavaClass() {
		return EntityAttribute.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.persistence.entityattribute.EntityAttribute";
	}

	// @Override
	public EntityAttribute readFrom(ProtoStreamReader reader) throws IOException {
		EntityAttribute bea = new EntityAttribute();
		bea.setRealm(reader.readString("realm"));
		bea.setBaseEntityCode(reader.readString("baseEntityCode"));
		bea.setAttributeCode(reader.readString("attributeCode"));
		Long createdLong = reader.readLong("created");
		if (createdLong != null) {
			bea.setCreated(LocalDateTime.ofEpochSecond(createdLong / 1000, 0, ZoneOffset.UTC));
		}
		bea.setInferred(reader.readBoolean("inferred"));
		bea.setPrivacyFlag(reader.readBoolean("privacyFlag"));
		bea.setReadonly(reader.readBoolean("readonly"));
		Long updatedLong = reader.readLong("updated");
		if (updatedLong != null) {
			bea.setUpdated(LocalDateTime.ofEpochSecond(updatedLong / 1000, 0, ZoneOffset.UTC));
		}
		bea.setValueBoolean(reader.readBoolean("valueBoolean"));
		Long valueDateLong = reader.readLong("valueDate");
		if (valueDateLong != null) {
			bea.setValueDate(LocalDateTime.ofEpochSecond(valueDateLong / 1000, 0, ZoneOffset.UTC).toLocalDate());
		}
		Long valueDateTimeLong = reader.readLong("valueDateTime");
		if (valueDateTimeLong != null) {
			bea.setValueDateTime(LocalDateTime.ofEpochSecond(valueDateTimeLong / 1000, 0, ZoneOffset.UTC));
		}
		bea.setValueDouble(reader.readDouble("valueDouble"));
		bea.setValueInteger(reader.readInt("valueInteger"));
		bea.setValueLong(reader.readLong("valueLong"));
		String moneyStr = reader.readString("money");
		if (!StringUtils.isEmpty(moneyStr) && !"null".equals(moneyStr)) {
			JsonObject jsonObj = (JsonObject) Json.decodeValue(moneyStr);
			if (jsonObj != null) {
				Money money = null;
				CurrencyUnit currency = Monetary.getCurrency(jsonObj.getString("currency"));
				Double amount = Double.valueOf(jsonObj.getString("amount"));
				if (amount != null && currency != null) {
					money = Money.of(amount, currency);
				}
				bea.setValueMoney(money);
			}
		}
		bea.setValueString(reader.readString("valueString"));
		Long valueTimeLong = reader.readLong("valueTime");
		if (valueTimeLong != null) {
			bea.setValueTime(LocalTime.ofInstant(Instant.ofEpochSecond(valueTimeLong / 1000), ZoneOffset.UTC));
		}
		bea.setWeight(reader.readDouble("weight"));
		bea.setConfirmationFlag(reader.readBoolean("confirmationFlag"));
		return bea;
	}

	// @Override
	public void writeTo(ProtoStreamWriter writer, EntityAttribute bea) throws IOException {
		writer.writeString("realm", bea.getRealm());
		writer.writeString("baseEntityCode", bea.getBaseEntityCode());
		writer.writeString("attributeCode", bea.getAttributeCode());
		LocalDateTime created = bea.getCreated();
		Long createdLong = created != null ? created.toEpochSecond(ZoneOffset.UTC) * 1000 : null;
		writer.writeLong("created", createdLong);
		writer.writeBoolean("inferred", bea.getInferred());
		writer.writeBoolean("privacyFlag", bea.getPrivacyFlag());
		writer.writeBoolean("readonly", bea.getReadonly());
		LocalDateTime updated = bea.getUpdated();
		Long updatedLong = updated != null ? updated.toEpochSecond(ZoneOffset.UTC) * 1000 : null;
		writer.writeLong("updated", updatedLong);
		writer.writeBoolean("valueBoolean", bea.getValueBoolean());
		LocalDate valueDate = bea.getValueDate();
		Long valueDateLong = valueDate != null ? valueDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000 : null;
		writer.writeLong("valueDate", valueDateLong);
		LocalDateTime valueDateTime = bea.getValueDateTime();
		Long valueDateTimeLong = valueDateTime != null ? valueDateTime.toEpochSecond(ZoneOffset.UTC) * 1000 : null;
		writer.writeLong("valueDateTime", valueDateTimeLong);
		writer.writeDouble("valueDouble", bea.getValueDouble());
		writer.writeInt("valueInteger", bea.getValueInteger());
		writer.writeLong("valueLong", bea.getValueLong());
		Money money = bea.getValueMoney();
		String moneyStr = null;
		if (money != null) {
			StringBuilder moneyJson = new StringBuilder();
			String currency = money.getCurrency().toString();
			String amount = money.getNumber().toString();
			moneyJson.append("{\"currency\":\"").append(currency).append("\", \"amount\":\"").append(amount)
					.append("\"}");
			moneyStr = moneyJson.toString();
		}
		writer.writeString("money", moneyStr);
		writer.writeString("valueString", bea.getValueString());
		writer.writeDouble("weight", bea.getWeight());
		writer.writeBoolean("confirmationFlag", bea.getConfirmationFlag());
	}

}
