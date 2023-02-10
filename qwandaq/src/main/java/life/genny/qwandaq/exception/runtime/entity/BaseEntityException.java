package life.genny.qwandaq.exception.runtime.entity;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.attribute.EntityAttribute;

import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.exception.runtime.NullParameterException;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.callbacks.FILogCallback;

/**
 * This Exception gets raised when there is an error directly related to the way a BaseEntity is configured for a given context
 */
public class BaseEntityException extends BadDataException {
    private static final Logger log = Logger.getLogger(BaseEntityException.class);

    public static final String DEFAULT_MESSAGE = "Error with BaseEntity: ";
    private static final String DEFAULT_DETAILS = "";

    private final BaseEntity baseEntity;

    /**
     * Create a new Exception with an attached {@link BaseEntity}, {@link Throwable cause} and a detailed message
     * @param baseEntity - BaseEntity with bad data
     * @param details - detailed error message describing the issue
     * @param cause - an existing cause related to this Exception
     */
    public BaseEntityException(BaseEntity baseEntity, String details, Throwable cause) {
		super(constructMessage(baseEntity, details), cause);
        this.baseEntity = baseEntity;
    }

    /**
     * Create a new Exception with an attached {@link BaseEntity}, {@link Throwable cause} but no detailed message
     * @param baseEntity - BaseEntity with bad data
     * @param cause - an existing cause related to this Exception
     */
	public BaseEntityException(BaseEntity baseEntity, Throwable cause) {
        this(baseEntity, DEFAULT_DETAILS, cause);
	}

    /**
     * Create a new Exception with an attached {@link BaseEntity}, {@link Throwable cause} and a detailed message
     * @param baseEntity - BaseEntity with bad data
     * @param details - detailed error message describing the issue
     * @param cause - an existing cause related to this Exception
     */
    public BaseEntityException(BaseEntity baseEntity, String details) {
        super(constructMessage(baseEntity, details));
        this.baseEntity = baseEntity;
    }

    /**
     * Create a new Exception with an attached {@link BaseEntity}, {@link Throwable cause} and a detailed message
     * @param baseEntity - BaseEntity with bad data
     * @param details - detailed error message describing the issue
     * @param cause - an existing cause related to this Exception
     */
    public BaseEntityException(BaseEntity baseEntity) {
        this(baseEntity, DEFAULT_DETAILS);
    }

    /**
     * Print the relevant {@link BaseEntity BaseEntity's} {@link EntityAttribute EntityAttributes}
     * @param logLevel - the {@link FILogCallback logLevel} to print on (example log::error)
     */
    public void printBaseEntityAttributes(FILogCallback logLevel) {
        if(baseEntity == null)
            throw new NullParameterException("baseEntity", this);
        CommonUtils.printCollection(baseEntity.getBaseEntityAttributes(), logLevel, ea -> {
            return ea.getBaseEntityCode() + ":" + ea.getAttributeCode() + " = " + ea.getValueString();
        });
    }

    /**
     * Print the relevant {@link BaseEntity BaseEntity's} {@Link EntityAttribute EntityAttributes} on log::error
     */
    public void printBaseEntityAttributes() {
        printBaseEntityAttributes(log::error);
    }

    /**
     * Construct and return the error message for this {@link Exception}
     * @param be the {@link BaseEntity} causing this Exception
     * @param details the details of the message (default: {@link BaseEntityException#DEFAULT_DETAILS DEFAULT_DETAILS})
     * @return the error message
     */
    private static String constructMessage(BaseEntity be, String details) {
        StringBuilder sb = new StringBuilder(DEFAULT_MESSAGE)
                            .append(be.getCode());
        
        if(!StringUtils.isBlank(details)) {
            sb.append(".\n")
              .append(details);
        }

        return sb.toString();
    }
}
