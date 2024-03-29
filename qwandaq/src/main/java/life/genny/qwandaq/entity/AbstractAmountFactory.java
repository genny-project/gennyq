package org.javamoney.moneta.spi;

import javax.money.*;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Basic implementation of {@link javax.money.MonetaryAmountFactory}, which simplifies development of the SPI interface.
 *
 * @author Varun Shastry
 * 
 * @param <T> the target class implementing {@link javax.money.MonetaryAmount}.
 */
public abstract class AbstractAmountFactory<T extends MonetaryAmount> implements MonetaryAmountFactory<T> {

    /**
     * The default {@link MonetaryContext} applied, if not set explicitly on creation.
     */
    private final MonetaryContext defaultMonetaryContext = loadDefaultMonetaryContext();

    /**
     * The default {@link MonetaryContext} applied, if not set explicitly on creation.
     */
    private final MonetaryContext maxMonetaryContext = loadMaxMonetaryContext();

    private CurrencyUnit currency;
    private Number number;
    private MonetaryContext monetaryContext = defaultMonetaryContext;

    /**
     * Creates a new instance of {@link MonetaryAmount}, using the default {@link MonetaryContext}.
     *
     * @return a {@code MonetaryAmount} combining the numeric value and currency unit.
     * @throws ArithmeticException If the number exceeds the capabilities of the default {@link MonetaryContext}
     *                             used.
     */
    @Override
    public T create() {
        if (currency == null) {
            throw new MonetaryException("Cannot create an instance of '"+this.getAmountType().getName()+"': missing currency.");
        }
        if (number == null) {
            throw new MonetaryException("Cannot create an instance of '"+this.getAmountType().getName()+"': missing number.");
        }
        if (monetaryContext == null) {
            throw new MonetaryException("Cannot create an instance of '"+this.getAmountType().getName()+"': missing context.");
        }
        return create(number, currency, monetaryContext);
    }

    protected abstract T create(Number number, CurrencyUnit currency, MonetaryContext monetaryContext);

    protected abstract MonetaryContext loadDefaultMonetaryContext();

    protected abstract MonetaryContext loadMaxMonetaryContext();


    
    /** 
     * @param currency
     * @return MonetaryAmountFactory<T>
     */
    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmountFactory#withCurrency(javax.money.CurrencyUnit)
     */
    @Override
    public MonetaryAmountFactory<T> setCurrency(CurrencyUnit currency) {
        Objects.requireNonNull(currency);
        this.currency = currency;
        return this;
    }

    
    /** 
     * @param number
     * @return MonetaryAmountFactory<T>
     */
    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmountFactory#with(java.lang.Number)
     */
    @Override
    public MonetaryAmountFactory<T> setNumber(Number number) {
        this.number = ConvertBigDecimal.of(number);
        return this;
    }

    
    /** 
     * @param currencyCode
     * @return MonetaryAmountFactory<T>
     */
    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmountFactory#withCurrency(java.lang.String)
     */
    @Override
    public MonetaryAmountFactory<T> setCurrency(String currencyCode) {
        this.currency = Monetary.getCurrency(currencyCode);
        return this;
    }

    /**
     * Creates a new instance of {@link Monetary}, using the default {@link MonetaryContext}.
     *
     * @param number numeric value.
     * @return a {@code Money} combining the numeric value and currency unit.
     * @throws ArithmeticException      If the number exceeds the capabilities of the default {@link MonetaryContext}
     *                                  used.
     * @throws UnknownCurrencyException if the currency code can not be resolved to {@link CurrencyUnit}.
     */
    @Override
    public MonetaryAmountFactory<T> setNumber(double number) {
        this.number = new BigDecimal(String.valueOf(number));
        return this;
    }

    
    /** 
     * @param number
     * @return MonetaryAmountFactory<T>
     */
    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmountFactory#with(long)
     */
    @Override
    public MonetaryAmountFactory<T> setNumber(long number) {
        this.number = BigDecimal.valueOf(number);
        return this;
    }

    
    /** 
     * @param monetaryContext
     * @return MonetaryAmountFactory<T>
     */
    /*
     * (non-Javadoc)
     * @see javax.money.MonetaryAmountFactory#with(javax.money.MonetaryContext)
     */
    @Override
    public MonetaryAmountFactory<T> setContext(MonetaryContext monetaryContext) {
        Objects.requireNonNull(monetaryContext);
        int maxScale = getMaximalMonetaryContext().getMaxScale();
        if (maxScale != -1 && maxScale < monetaryContext.getMaxScale()) {
            throw new MonetaryException(
                    "Context exceeds maximal capabilities (scale) of this type: " + monetaryContext);
        }
        int precision = getMaximalMonetaryContext().getPrecision();
        if (precision != 0 && precision < monetaryContext.getPrecision()) {
            throw new MonetaryException(
                    "Contexts exceeds maximal capabilities (precision) of this type: " + monetaryContext);
        }
        this.monetaryContext = monetaryContext;
        return this;
    }

    /**
     * Returns the default {@link MonetaryContext} used, when no {@link MonetaryContext} is
     * provided.
     *
     * @return the default {@link MonetaryContext}, never {@code null}.
     */
    @Override
    public MonetaryContext getDefaultMonetaryContext() {
        return defaultMonetaryContext;
    }

    /**
     * Returns the maximal {@link MonetaryContext} supported.
     *
     * @return the maximal {@link MonetaryContext}, never {@code null}.
     */
    @Override
    public MonetaryContext getMaximalMonetaryContext() {
        return maxMonetaryContext;
    }

    /**
     * Converts (if necessary) the given {@link MonetaryAmount} to a new {@link MonetaryAmount}
     * instance, hereby supporting the {@link MonetaryContext} given.
     *
     * @param smount the amount to be converted, if necessary.
     * @return an according Money instance.
     */
    @Override
    public MonetaryAmountFactory<T> setAmount(MonetaryAmount smount) {
        this.currency = smount.getCurrency();
        this.number = smount.getNumber().numberValue(BigDecimal.class);
        this.monetaryContext = MonetaryContextBuilder.of(defaultMonetaryContext.getAmountType())
                .importContext(smount.getContext()).build();
        return this;
    }

    /**
     * Creates a {@link BigDecimal} from the given {@link Number} doing the valid conversion
     * depending the type given.
     *
     * @param num the number type
     * @return the corresponding {@link BigDecimal}
     * @deprecated will be removed in next release
     */
    @Deprecated
    protected static BigDecimal getBigDecimal(Number num) {
        return ConvertBigDecimal.of(num);
    }

}
