package org.xvm.asm.constants;


import org.xvm.asm.Annotation;
import org.xvm.asm.ConstantPool;
import org.xvm.asm.Constants.Access;


/**
 * Represents the compile time and runtime information (aggregated across all contributions and
 * virtual levels) about a single property as it appears in a particular type.
 */
public class PropertyInfo
    {
    /**
     * Construct a PropertyInfo from the passed information.
     *
     * @param constId      the property constant
     * @param type         the type of the property, including any type annotations (required)
     * @param fRO          true iff the property is a Ref; false iff the property is a Var
     * @param aPropAnno    an array of non-virtual annotations on the property declaration itself
     * @param aRefAnno     an array of annotations that apply to the Ref/Var of the property
     * @param fCustomCode  true to indicate that the property has custom code that overrides the
     *                     underlying Ref/Var implementation
     * @param fReqField    true iff the property requires the presence of a field
     * @param fAbstract
     * @param fTrailingOverride
     */
    public PropertyInfo(PropertyConstant constId, TypeConstant type, boolean fRO,
            Annotation[] aPropAnno, Annotation[] aRefAnno, boolean fCustomCode, boolean fReqField,
            boolean fAbstract, boolean fTrailingOverride)
        {
        assert constId != null;
        assert type != null;

        m_constId   = constId;
        m_type      = type;
        m_paraminfo = null;
        m_fRO       = fRO;
        m_aPropAnno = TypeInfo.validateAnnotations(aPropAnno);
        m_aRefAnno  = TypeInfo.validateAnnotations(aRefAnno);
        m_fCustom   = fCustomCode;
        m_fField    = fReqField;
        m_fAbstract = fAbstract;
        m_fOverride = fTrailingOverride;
        }

    /**
     * Construct a PropertyInfo that represents the specified type parameter.
     *
     * @param constId  the identity of this property
     * @param param    the type parameter information
     */
    public PropertyInfo(PropertyConstant constId, ParamInfo param)
        {
        ConstantPool pool = constId.getConstantPool();

        m_constId   = constId;
        m_type      = pool.ensureParameterizedTypeConstant(pool.typeType(), param.getConstraintType());
        m_paraminfo = param;
        m_fRO       = true;
        m_aPropAnno = Annotation.NO_ANNOTATIONS;
        m_aRefAnno  = Annotation.NO_ANNOTATIONS;
        m_fCustom   = false;
        m_fField    = false;
        m_fAbstract = false;
        m_fOverride = false;
        }

    /**
     * Combine the information in this PropertyInfo with the information from a super type's
     * PropertyInfo.
     *
     * @param that  a super-type's PropertyInfo
     *
     * @return a PropertyInfo representing the combined information
     */
    public PropertyInfo combineWithSuper(PropertyInfo that)
        {
        if (this.isTypeParam() || that.isTypeParam())
            {
            if (this.isTypeParam() && that.isTypeParam() && this.getType().isA(that.getType()))
                {
                return this;
                }

            throw new IllegalStateException(
                    "cannot combine PropertyInfo objects if either represents a type parameter");
            }

        assert this.getName().equals(that.getName());
        assert this.m_type.isA(that.m_type);

        return new PropertyInfo(
                this.m_constId,
                this.m_type,
                this.m_fRO & that.m_fRO,                // read-only Ref if both are read-only
                this.m_aPropAnno,                       // property annotations NOT inherited
                TypeInfo.mergeAnnotations(this.m_aRefAnno, that.m_aRefAnno),
                this.m_fCustom | that.m_fCustom,        // custom logic if either is custom
                this.m_fField | that.m_fField,          // field present if either has field
                this.m_fAbstract,                       // abstract if the top one is abstract
                that.m_fOverride);                      // override if the bottom one is override
        }

    /**
     * Create a new PropertyInfo that represents a more limited (public or protected) access to the
     * members of this property that is on the private type.
     *
     * @param access  the desired access, either PUBLIC or PROTECTED
     *
     * @return a PropertyInfo to use, or null if the PropertyInfo would not be present on the type
     *         with the specified access
     */
    public PropertyInfo limitAccess(Access access)
        {
        // TODO this property is either a Var or a Ref on the private type (i.e. this PropertyInfo)
        //      determine if the same property would be a Var, a Ref, or absent from the type with the specified access
        //      - if absent, return null
        //      - if the same as on the private type, then return this
        //      - otherwise private type must be Var and we need to create a Ref (@RO) of the same
        return this;
        }

    /**
     * Specifies a value for whether or not the property has a field, and unsets the abstract flag.
     *
     * @param fField  true to indicate the presence of a field
     *
     * @return the PropertyInfo reflecting the changes
     */
    public PropertyInfo specifyField(boolean fField)
        {
        return new PropertyInfo(m_constId, m_type, m_fRO, m_aPropAnno, m_aRefAnno,
                m_fCustom, fField, false, m_fOverride);
        }

    /**
     * Specifies a value for whether or not the property overrides some unknown super property.
     *
     * @param fOverride  specifies whether the resulting PropertyInfo is overriding
     *
     * @return the PropertyInfo reflecting the changes
     */
    public PropertyInfo specifyOverride(boolean fOverride)
        {
        return new PropertyInfo(m_constId, m_type, m_fRO, m_aPropAnno, m_aRefAnno,
                m_fCustom, m_fField, m_fAbstract, fOverride);
        }

    /**
     * @return the container of the property
     */
    public IdentityConstant getParent()
        {
        return m_constId.getParentConstant();
        }

    /**
     * @return the identity of the property
     */
    public PropertyConstant getIdentity()
        {
        return m_constId;
        }

    /**
     * @return the property name
     */
    public String getName()
        {
        return m_constId.getName();
        }

    /**
     * @return the property type
     */
    public TypeConstant getType()
        {
        return m_type;
        }

    /**
     * @return true iff this property represents a type parameter type
     */
    public boolean isTypeParam()
        {
        return m_paraminfo != null;
        }

    /**
     * @return the type param info
     */
    public ParamInfo getParamInfo()
        {
        return m_paraminfo;
        }

    /**
     * @return true iff this property is a Ref; false iff this property is a Var
     */
    public boolean isRO()
        {
        return m_fRO;
        }

    /**
     * @return true iff this property has a field, whether or not that field is reachable
     */
    public boolean hasField()
        {
        return m_fField;
        }

    /**
     * @return an array of the non-virtual annotations on the property declaration itself
     */
    public Annotation[] getPropertyAnnotations()
        {
        return m_aPropAnno;
        }

    /**
     * @return an array of the annotations that apply to the Ref/Var of the property
     */
    public Annotation[] getRefAnnotations()
        {
        return m_aRefAnno;
        }

    /**
     * @return true iff the property has any methods in addition to the underlying Ref or Var
     *         "rebasing" implementation, and in addition to any annotations
     */
    public boolean isCustomLogic()
        {
        return m_fCustom;
        }

    /**
     * @return the MethodConstant that will identify the getter (but not necessarily a
     *         MethodConstant that actually exists, because there may not be a getter, but also
     *         because the fully resolved type is used in the MethodConstant)
     */
    public MethodConstant getGetterId()
        {
        ConstantPool pool = m_constId.getConstantPool();
        return pool.ensureMethodConstant(m_constId, "get", ConstantPool.NO_TYPES, new TypeConstant[]{m_type});
        }

    /**
     * @return the MethodConstant that will identify the setter (but not necessarily a
     *         MethodConstant that actually exists, because there may not be a setter, but also
     *         because the fully resolved type is used in the MethodConstant)
     */
    public MethodConstant getSetterId()
        {
        ConstantPool pool = m_constId.getConstantPool();
        return pool.ensureMethodConstant(m_constId, "set", new TypeConstant[]{m_type}, ConstantPool.NO_TYPES);
        }

    /**
     * @return true iff the property is abstract, which means that it comes from an interface
     */
    public boolean isAbstract()
        {
        return m_fAbstract;
        }

    /**
     * @return true iff the property is abstract, which means that it comes from an interface
     */
    public boolean isOverride()
        {
        return m_fOverride;
        }

    /**
     * @return true if the property is annotated by "@Abstract"
     */
    public boolean isExplicitAbstract()
        {
        return TypeInfo.containsAnnotation(m_aPropAnno, "Abstract");
        }

    /**
     * @return true if the property is annotated by "@Override"
     */
    public boolean isExplicitOverride()
        {
        return TypeInfo.containsAnnotation(m_aPropAnno, "Override");
        }

    /**
     * @return true if the property is annotated by "@RO"
     */
    public boolean isExplicitReadOnly()
        {
        return TypeInfo.containsAnnotation(m_aPropAnno, "RO");
        }

    /**
     * @return true if the property is annotated by "@Inject"
     */
    public boolean isExplicitInject()
        {
        return TypeInfo.containsAnnotation(m_aRefAnno, "Inject");
        }

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        // TODO needs some work

        if (m_fRO)
            {
            sb.append("@RO ");
            }

        sb.append(m_type.getValueString())
                .append(' ')
                .append(getName());

        return sb.toString();
        }


    // ----- fields --------------------------------------------------------------------------------

    /**
     * The property's identity constant.
     */
    private final PropertyConstant m_constId;

    /**
     * Type of the property, including any annotations on the type.
     */
    private final TypeConstant m_type;

    /**
     * Type parameter information.
     */
    private final ParamInfo m_paraminfo;

    /**
     * True iff the property is a Ref; false iff the property is a Var.
     */
    private final boolean m_fRO;

    /**
     * An array of non-virtual annotations on the property declaration itself
     */
    private final Annotation[] m_aPropAnno;

    /**
     * An array of annotations that apply to the Ref/Var of the property.
     */
    private final Annotation[] m_aRefAnno;

    /**
     * True to indicate that the property has custom code that overrides the underlying Ref/Var
     * implementation.
     */
    private final boolean m_fCustom;

    /**
     * True iff the property requires a field.
     */
    private final boolean m_fField;

    /**
     * True iff the property is abstract, such as when it is on an interface.
     */
    private final boolean m_fAbstract;

    /**
     * True iff the property's last contribution specifies "@Override".
     */
    private final boolean m_fOverride;
    }
