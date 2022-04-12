package org.javamoney.moneta.spi;

import java.math.RoundingMode;

import javax.money.CurrencyUnit;
import javax.money.MonetaryContext;
import javax.money.MonetaryContextBuilder;
import javax.money.NumberValue;

import org.javamoney.moneta.Money;
import org.javamoney.moneta.spi.AbstractAmountFactory;

/**
 * Implementation of {@link javax.money.MonetaryAmountFactory} creating instances of {@link Money}.
 *
 * @author Varun Shastry
 */
public class MoneyAmountFactory extends AbstractAmountFactory<Money> {

    static final MonetaryContext DEFAULT_CONTEXT =
            MonetaryContextBuilder.of(Money.class).set(64).setMaxScale(63).set(RoundingMode.HALF_EVEN).build();
    static final MonetaryContext MAX_CONTEXT =
            MonetaryContextBuilder.of(Money.class).setPrecision(0).setMaxScale(-1).set(RoundingMode.HALF_EVEN).build();

    
    /** 
     * @param number
     * @param currency
     * @param monetaryContext
     * @return Money
     */
    @Override
    protected Money create(Number number, CurrencyUnit currency, MonetaryContext monetaryContext) {
        return Money.of(number, currency, MonetaryContext.from(monetaryContext, Money.class));
    }

    
    /** 
     * @return NumberValue
     */
    @Override
    public NumberValue getMaxNumber() {
        return null;
    }

    
    /** 
     * @return NumberValue
     */
    @Override
    public NumberValue getMinNumber() {
        return null;
    }

    
    /** 
     * @return Class<Money>
     */
    @Override
    public Class<Money> getAmountType() {
        return Money.class;
    }

    
    /** 
     * @return MonetaryContext
     */
    @Override
    protected MonetaryContext loadDefaultMonetaryContext() {
        return DEFAULT_CONTEXT;
    }

    
    /** 
     * @return MonetaryContext
     */
    @Override
    protected MonetaryContext loadMaxMonetaryContext() {
        return MAX_CONTEXT;
    }

}
