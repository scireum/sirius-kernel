package sirius.kernel.commons;

/**
 * Represents a boolean state variable, which can be toggled once from <tt>false</tt> to <tt>true</tt>.
 * <p>
 * All successive calls will not change the internal state, and return constantly true (or respectively).
 * <p>
 * This is particularly useful in loops where the first iteration must be handled differently. Therefore two more
 * functions are provided: {@link #firstCall()} and {@link #successiveCall()} which can be used like shown here:
 * <pre>
 * <code>
 * Monoflop mono = Monoflop.create();
 * StringBuilder sb = new StringBuilder();
 * for(String value : somelist) {
 *     if (mono.successiveCall()) {
 *         sb.append(", ");
 *     }
 *     sb.append(value);
 * }
 * System.out.println(sb.toString());
 * </code>
 * </pre>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
public class Monoflop {

    private boolean toggled = false;

    /*
     * Use the static constructor create()
     */
    private Monoflop() {

    }

    /**
     * Creates a new monoflop.
     *
     * @return a new monoflop which was not toggled yet.
     */
    public static Monoflop create() {
        return new Monoflop();
    }

    /**
     * Toggles the monoflop and returns its state <b>before</b> it was toggled.
     * <p>
     * If the monoflop is in its initial state, this will return <tt>false</tt> (and toggle the monoflop).
     * Therefore all successive class will return <tt>true</tt>.
     *
     * @return <tt>false</tt> if the monoflop was not toggled yet, <tt>true</tt> for all successive calls
     */
    public boolean toggle() {
        if (toggled) {
            return true;
        }
        toggled = true;
        return false;
    }

    /**
     * Toggles the monoflop and returns its state <b>before</b> it was toggled.
     * <p>
     * If the monoflop is in its initial state, this will return <tt>true</tt> (and toggle the monoflop).
     * Therefore all successive class will return <tt>false</tt>.
     *
     * @return <tt>true</tt> if the monoflop was not toggled yet, <tt>false</tt> for all successive calls
     */
    public boolean inverseToggle() {
        return !toggle();
    }

    /**
     * Reads and then toggles the monoflop.
     * <p>
     * If the monoflop is in its initial state, this will return <tt>true</tt> (and toggle the monoflop).
     * Therefore all successive class will return <tt>false</tt>.
     * <p>
     * This is just an alias for {@link #inverseToggle()}.
     *
     * @return <tt>true</tt> if the monoflop was not toggled yet, <tt>false</tt> for all successive calls
     */
    public boolean firstCall() {
        return inverseToggle();
    }

    /**
     * Reads and then toggles the monoflop.
     * <p>
     * If the monoflop is in its initial state, this will return <tt>false</tt> (and toggle the monoflop).
     * Therefore all successive class will return <tt>true</tt>.
     * <p>
     * This is just an alias for {@link #toggle()}.
     *
     * @return <tt>false</tt> if the monoflop was not toggled yet, <tt>true</tt> for all successive calls
     */
    public boolean successiveCall() {
        return toggle();
    }

    /**
     * Reads the internal state <b>without</b> toggling it.
     *
     * @return <tt>false</tt> if the monoflop is in its initial state, <tt>true</tt> otherwise
     */
    public boolean isToggled() {
        return toggled;
    }

    @Override
    public String toString() {
        return "Monoflop (Toggled: " + toggled + ")";
    }

}
