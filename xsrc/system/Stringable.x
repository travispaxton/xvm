/**
 * An interface that allows a class to optimize its rendering into a String.
 */
interface Stringable
    {
    /**
     * Estimate the number of characters that this Stringable object will use in its String form.
     *
     * @return the number of characters that this object estimates it will need for its String form
     */
    Int estimateStringLength();

    /**
     * Append the String form of this Stringable object to the provided character Appender.
     *
     * @param appender  the Appender to append the String form of this object to
     */
    void appendTo(Appender<Char> appender);
    }
