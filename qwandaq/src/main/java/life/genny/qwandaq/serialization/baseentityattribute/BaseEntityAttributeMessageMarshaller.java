package life.genny.qwandaq.serialization.baseentityattribute;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

import org.infinispan.protostream.MessageMarshaller;
import org.javamoney.moneta.Money;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class BaseEntityAttributeMessageMarshaller implements MessageMarshaller<BaseEntityAttribute> {

	@Override
	public Class<BaseEntityAttribute> getJavaClass() {
		return BaseEntityAttribute.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.serialization.baseentityattribute.BaseEntityAttribute";
	}

	// @Override
	public BaseEntityAttribute readFrom(ProtoStreamReader reader) throws IOException {
		BaseEntityAttribute bea = new BaseEntityAttribute();
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
		Money money = null;
		JsonObject jsonObj = (JsonObject) Json.decodeValue(reader.readString("money"));
		if (jsonObj != null) {
			CurrencyUnit currency = Monetary.getCurrency(jsonObj.getString("currency"));
			Double amount = Double.valueOf(jsonObj.getString("amount"));
			if (amount != null && currency != null) {
				money = Money.of(amount, currency);
			}
		}
		bea.setMoney(money);
		bea.setValueString(reader.readString("valueString"));
		Long valueTimeLong = reader.readLong("valueTime");
		if (valueTimeLong != null) {
			bea.setUpdated(LocalDateTime.ofEpochSecond(valueTimeLong / 1000, 0, ZoneOffset.UTC));
		}
		bea.setWeight(reader.readDouble("weight"));
		bea.setAttributeId(reader.readLong("attribute_id"));
		bea.setBaseEntityId(reader.readLong("baseentity_id"));
		bea.setIcon(reader.readString("icon"));
		bea.setConfirmationFlag(reader.readBoolean("confirmationFlag"));
		return bea;
	}

	// @Override
	public void writeTo(ProtoStreamWriter writer, BaseEntityAttribute bea) throws IOException {
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
		Money money = bea.getMoney();
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
		/*
		 * LocalTime valueTime = bea.getValueTime(); Long valueTimeLong = created !=
		 * null ? valueTime.toEpochSecond(ZoneOffset.UTC)*1000 : null;
		 * writer.writeLong("updated", valueTimeLong);
		 */
		writer.writeDouble("weight", bea.getWeight());
		writer.writeLong("attribute_id", bea.getAttributeId());
		writer.writeLong("baseentity_id", bea.getBaseEntityId());
		writer.writeString("icon", bea.getIcon());
		writer.writeBoolean("confirmationFlag", bea.getConfirmationFlag());
	}

}
