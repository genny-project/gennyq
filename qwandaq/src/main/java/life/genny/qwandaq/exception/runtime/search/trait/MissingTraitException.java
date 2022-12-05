package life.genny.qwandaq.exception.runtime.search.trait;

import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.exception.runtime.search.SearchException;

public class MissingTraitException extends SearchException {

    public MissingTraitException(SearchEntity entity, String traitCode) {
        super(entity, new StringBuilder("Missing Trait: ")
                            .append(traitCode)
                            .toString());
    }
    
}
