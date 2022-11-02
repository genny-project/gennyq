package life.genny.qwandaq.utils.ickle;

public enum IckleClause {
    /**
     * The insert values clause
     */
    INSERT,

    /**
     * The update set clause
     */
    UPDATE,

    /**
     * Not used in 5.x.  Intended for use in 6+ as indicator
     * of processing predicates (where clause) that occur in a
     * delete
     */
    DELETE,

    SELECT,
    FROM,
    WHERE,
    GROUP,
    HAVING,
    ORDER,
    LIMIT

}