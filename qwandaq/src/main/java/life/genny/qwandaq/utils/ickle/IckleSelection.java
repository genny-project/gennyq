package life.genny.qwandaq.utils.ickle;

import javax.persistence.TupleElement;
import java.util.List;

/**
 * The <code>Selection</code> interface defines an item that is to be
 * returned in a query result.
 *
 * @param <X> the type of the selection item
 *
 * @since 2.0
 */
public interface IckleSelection<X> {

    /**
     * Return the Java type of the selection.
     * @return the Java type of the selection
     */
    Class<? extends X> getJavaType();

    /**
     * Return the alias assigned to the selection or null,
     * if no alias has been assigned.
     * @return alias
     */
    String getAlias();

    /**
     * Assigns an alias to the selection item.
     * Once assigned, an alias cannot be changed or reassigned.
     * Returns the same selection item.
     * @param name  alias
     * @return selection item
     */
    IckleSelection<X> alias(String name);

    /**
     * Whether the selection item is a compound selection.
     * @return boolean indicating whether the selection is a compound
     *         selection
     */
    boolean isCompoundSelection();

    /**
     * Return the selection items composing a compound selection.
     * Modifications to the list do not affect the query.
     * @return list of selection items
     * @throws IllegalStateException if selection is not a
     *         compound selection
     */
    List<IckleSelection<?>> getCompoundSelectionItems();
}
