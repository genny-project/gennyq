package life.genny.qwandaq.serialization.entityentity;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

import life.genny.qwandaq.Link;
import org.infinispan.protostream.MessageMarshaller;
import org.javamoney.moneta.Money;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class EntityEntityMessageMarshaller implements MessageMarshaller<EntityEntity> {

	@Override
	public Class<EntityEntity> getJavaClass() {
		return EntityEntity.class;
	}

	@Override
	public String getTypeName() {
		return "life.genny.qwandaq.persistence.entityentity.EntityEntity";
	}

	// @Override
	public EntityEntity readFrom(ProtoStreamReader reader) throws IOException {
		EntityEntity ee = new EntityEntity();
		ee.setTargetCode(reader.readString("targetCode"));
		Long createdLong = reader.readLong("created");
		if (createdLong != null) {
			ee.setCreated(LocalDateTime.ofEpochSecond(createdLong / 1000, 0, ZoneOffset.UTC));
		}
		ee.setLinkCode(reader.readString("LINK_CODE"));
		ee.setChildColor(reader.readString("childColor"));
		ee.setLinkValue(reader.readString("linkValue"));
		ee.setParentColor(reader.readString("parentColor"));
		ee.setRule(reader.readString("RULE"));
		ee.setSourceCode(reader.readString("SOURCE_CODE"));
		ee.setTarget_code(reader.readString("TARGET_CODE"));
		ee.setLinkWeight(reader.readDouble("LINK_WEIGHT"));
		ee.setRealm(reader.readString("realm"));
		Long updatedLong = reader.readLong("updated");
		if (updatedLong != null) {
			ee.setUpdated(LocalDateTime.ofEpochSecond(updatedLong / 1000, 0, ZoneOffset.UTC));
		}
		ee.setValueBoolean(reader.readBoolean("valueBoolean"));
		Long valueDateLong = reader.readLong("valueDate");
		if (valueDateLong != null) {
			ee.setValueDate(LocalDateTime.ofEpochSecond(valueDateLong / 1000, 0, ZoneOffset.UTC).toLocalDate());
		}
		Long valueDateTimeLong = reader.readLong("valueDateTime");
		if (valueDateTimeLong != null) {
			ee.setValueDateTime(LocalDateTime.ofEpochSecond(valueDateTimeLong / 1000, 0, ZoneOffset.UTC));
		}
		ee.setValueDouble(reader.readDouble("valueDouble"));
		ee.setValueInteger(reader.readInt("valueInteger"));
		ee.setValueLong(reader.readLong("valueLong"));
		Money money = null;
		JsonObject jsonObj = (JsonObject) Json.decodeValue(reader.readString("money"));
		if (jsonObj != null) {
			CurrencyUnit currency = Monetary.getCurrency(jsonObj.getString("currency"));
			Double amount = Double.valueOf(jsonObj.getString("amount"));
			if (amount != null && currency != null) {
				money = Money.of(amount, currency);
			}
		}
		ee.setMoney(money);
		ee.setValueString(reader.readString(""));
		Long valueTimeLong = reader.readLong("valueTime");
		if (valueTimeLong != null) {
			ee.setValueTime(LocalTime.of(valueTimeLong.intValue() / 1000, 0));
		}
		ee.setVersion(reader.readLong("version"));
		ee.setWeight(reader.readDouble("weight"));
		return ee;
	}

	// @Override
	public void writeTo(ProtoStreamWriter writer, EntityEntity ee) throws IOException {
		writer.writeString("targetCode", ee.getTargetCode());
		LocalDateTime created = ee.getCreated();
		Long createdLong = created != null ? created.toEpochSecond(ZoneOffset.UTC) * 1000 : null;
		writer.writeLong("created", createdLong);
		writer.writeString("LINK_CODE", ee.getLinkCode());
		writer.writeString("childColor", ee.getChildColor());
		writer.writeString("linkValue", ee.getLinkValue());
		writer.writeString("parentColor", ee.getParentColor());
		writer.writeString("RULE", ee.getRule());
		writer.writeString("SOURCE_CODE", ee.getSourceCode());
		writer.writeString("TARGET_CODE", ee.getTarget_code());
		writer.writeDouble("LINK_WEIGHT",ee.getLinkWeight());
		writer.writeString("realm", ee.getRealm());
		LocalDateTime updated = ee.getUpdated();
		Long updatedLong = updated != null ? updated.toEpochSecond(ZoneOffset.UTC) * 1000 : null;
		writer.writeLong("updated", updatedLong);
		writer.writeBoolean("valueBoolean", ee.getValueBoolean());
		LocalDate valueDate = ee.getValueDate();
		Long valueDateLong = valueDate != null ? valueDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000 : null;
		writer.writeLong("valueDate", valueDateLong);
		LocalDateTime valueDateTime = ee.getValueDateTime();
		Long valueDateTimeLong = valueDateTime != null ? valueDateTime.toEpochSecond(ZoneOffset.UTC) * 1000 : null;
		writer.writeLong("valueDateTime", valueDateTimeLong);
		writer.writeDouble("valueDouble", ee.getValueDouble());
		writer.writeInt("valueInteger", ee.getValueInteger());
		writer.writeLong("valueLong", ee.getValueLong());
		Money money = ee.getMoney();
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
		writer.writeString("valueString", ee.getValueString());
		LocalTime valueTime = ee.getValueTime();
		Long valueTimeLong = Long.valueOf(valueTime != null ? valueTime.toSecondOfDay() * 1000 : null);
		writer.writeLong("valueTime", valueTimeLong);
		writer.writeLong("version", ee.getVersion());
		writer.writeDouble("weight", ee.getWeight());
	}

}
