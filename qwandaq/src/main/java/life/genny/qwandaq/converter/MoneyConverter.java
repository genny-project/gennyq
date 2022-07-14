package life.genny.qwandaq.converter;

import java.io.StringReader;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.jboss.logging.Logger;
import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonObject;

@Converter
public class MoneyConverter implements AttributeConverter<Money, String> {

	private static final Logger log = Logger.getLogger(MoneyConverter.class);

	/** 
	 * Convert a Money object to a String
	 *
	 * @param money the string to convert
	 * @return String
	 */
	@Override
	public String convertToDatabaseColumn(final Money money) {
		if (money == null) {
			return null;
		}
		return "{\"amount\":" + money.getNumber() + ",\"currency\":\"" + money.getCurrency().getCurrencyCode() + "\"}";
	}
	
	/** 
	 * Convert a String representation of Money to a Money object
	 *
	 * @param moneyStr the String to convert
	 * @return Money
	 */
	@Override
	public Money convertToEntityAttribute(String moneyStr) {

		if (moneyStr == null || moneyStr.equals("null")) {
			return null;
		}
		JsonReader reader = Json.createReader(new StringReader(moneyStr));
		JsonObject obj = reader.readObject();

		CurrencyUnit currency = Monetary.getCurrency(obj.getString("currency"));
		Double amount = Double.valueOf(obj.getString("amount"));

		Money money = Money.of(amount, currency);
		return money;
	}


}
