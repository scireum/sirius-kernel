package sirius.kernel.di.std;

/**
 * Marks a class as sortable by its priority.
 * <p>
 * Classes implementing this interface can be used by the {@link PriorityParts} annotation and will
 * be auto sorted (ascending) by priority before they are made available.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/02
 */
public interface Priorized {

    static final int DEFAULT_PRIORITY = 100;

    /**
     * Returns the priority of this element.
     *
     * @return the priority - lower is better (comes first)
     */
    int getPriority();

}
