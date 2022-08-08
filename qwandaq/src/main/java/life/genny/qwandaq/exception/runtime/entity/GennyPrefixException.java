package life.genny.qwandaq.exception.runtime.entity;

import life.genny.qwandaq.exception.GennyRuntimeException;

public class GennyPrefixException extends GennyRuntimeException {
    
    public GennyPrefixException(String code) {
        super(code + " does not have a valid prefix!");
    }

}
