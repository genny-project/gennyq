package org.javamoney.moneta.format;

import org.javamoney.moneta.function.MonetaryAmountProducer;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import javax.money.format.AmountFormatContext;
import javax.money.format.AmountFormatContextBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryParseException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The implementation that uses the {@link DecimalFormat} as formatter.
 *
 * @author Varun Shastry
 */
public class MonetaryAmountDecimalFormat implements MonetaryAmountFormat {

    static final String STYLE = "MonetaryAmountFormatSymbols";

    private static final MonetaryAmountFormat DEFAULT_INSTANCE = MonetaryAmountDecimalFormatBuilder.newInstance().build();

    private static final AmountFormatContext CONTEXT = AmountFormatContextBuilder.of(STYLE).build();

    private final DecimalFormat decimalFormat;

    private final MonetaryAmountProducer producer;

    private final CurrencyUnit currencyUnit;

    public MonetaryAmountDecimalFormat(DecimalFormat decimalFormat, MonetaryAmountProducer producer, CurrencyUnit currencyUnit) {
        this.decimalFormat = decimalFormat;
        this.producer = producer;
        this.currencyUnit = currencyUnit;
    }

    /**
     * Get the default formatter based on the current default locale.
     * @return
     */
    public static MonetaryAmountFormat of() {
        return DEFAULT_INSTANCE;
    }


    
    /** 
     * @return DecimalFormat
     */
    DecimalFormat getDecimalFormat() {
        return decimalFormat;
    }

    
    /** 
     * @return MonetaryAmountProducer
     */
    MonetaryAmountProducer getProducer() {
        return producer;
    }

    
    /** 
     * @return CurrencyUnit
     */
    CurrencyUnit getCurrencyUnit() {
        return currencyUnit;
    }


    
    /** 
     * @return String
     */
    public String toLocalizedPattern() {
        return decimalFormat.toLocalizedPattern();
    }

    
    /** 
     * @return String
     */
    public String toPattern() {
        return decimalFormat.toPattern();
    }

    
    /** 
     * @return AmountFormatContext
     */
    @Override
    public AmountFormatContext getContext() {
        return CONTEXT;
    }

    
    /** 
     * @param appendable
     * @param amount
     * @throws IOException
     */
    @Override
    public void print(Appendable appendable, MonetaryAmount amount) throws IOException {
        requireNonNull(appendable).append(queryFrom(amount));
    }

    
    /** 
     * @param text
     * @return MonetaryAmount
     * @throws MonetaryParseException
     */
    @Override
    public MonetaryAmount parse(CharSequence text) throws MonetaryParseException {
        requireNonNull(text);
        try {
            Number number = decimalFormat.parse(text.toString());
            return producer.create(currencyUnit, number);
        } catch (Exception exception) {
            throw new MonetaryParseException(exception.getMessage(), text, 0);
        }
    }

    
    /** 
     * @param amount
     * @return String
     */
    @Override
    public String queryFrom(MonetaryAmount amount) {
        return Optional
                .ofNullable(amount)
                .map(m -> decimalFormat.format(amount.getNumber().numberValue(
                        BigDecimal.class))).orElse("null");
    }

    
    /** 
     * @return int
     */
    @Override
    public int hashCode() {
        return Objects.hash(decimalFormat, currencyUnit, producer);
    }

    
    /** 
     * @param obj
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MonetaryAmountDecimalFormat other) {
            return Objects.equals(other.decimalFormat, decimalFormat) && Objects.equals(other.producer, producer)
                    && Objects.equals(other.currencyUnit, currencyUnit);
        }
        return false;
    }

    
    /** 
     * @return String
     */
    @Override
    public String toString() {
        return MonetaryAmountDecimalFormat.class.getName() + '{' +
                " decimalFormat: " + decimalFormat + ',' +
                " producer: " + producer + ',' +
                " currencyUnit: " + currencyUnit + '}';
    }
}
