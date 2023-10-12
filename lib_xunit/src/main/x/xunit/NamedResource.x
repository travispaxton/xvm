/**
 * A named and typed resource.
 */
interface NamedResource<DeclaredType, Resource extends DeclaredType> {
    /**
     * The name of the resource.
     */
    @RO String name;
    /**
     * The declared type of the resource
     */
    @RO Type<DeclaredType> type;
    /**
     * The actual resource.
     */
    @RO Resource resource;
}
