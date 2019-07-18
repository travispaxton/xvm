package org.xvm.asm.constants;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.function.Consumer;

import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;

import static org.xvm.util.Handy.readMagnitude;
import static org.xvm.util.Handy.writePackedLong;


/**
 * A NamedConstant is a constant whose purpose is to identify a structure of a specified name that
 * exists within its parent structure.
 */
public abstract class NamedConstant
        extends IdentityConstant
    {
    // ----- constructors --------------------------------------------------------------------------

    /**
     * Construct a constant whose purpose is to identify a structure of a specified name that exists
     * within its parent structure.
     *
     * @param pool         the ConstantPool that will contain this Constant
     * @param constParent  the module, package, class, or method that contains this property
     * @param sName        the property name
     */
    public NamedConstant(ConstantPool pool, IdentityConstant constParent, String sName)
        {
        super(pool);

        if (constParent == null)
            {
            throw new IllegalArgumentException("parent required");
            }

        if (sName == null)
            {
            throw new IllegalArgumentException("name required");
            }

        m_constParent = constParent;
        m_constName   = pool.ensureStringConstant(sName);
        }

    /**
     * Constructor used for deserialization.
     *
     * @param pool    the ConstantPool that will contain this Constant
     * @param format  the format of the Constant in the stream
     * @param in      the DataInput stream to read the Constant value from
     *
     * @throws IOException  if an issue occurs reading the Constant value
     */
    public NamedConstant(ConstantPool pool, Format format, DataInput in)
            throws IOException
        {
        super(pool);

        m_iParent = readMagnitude(in);
        m_iName   = readMagnitude(in);
        }

    @Override
    protected void resolveConstants()
        {
        ConstantPool pool = getConstantPool();

        m_constParent = (IdentityConstant) pool.getConstant(m_iParent);
        m_constName   = (StringConstant)   pool.getConstant(m_iName);
        }


    // ----- type-specific functionality -----------------------------------------------------------

    /**
     * Replace the parent constant with an equivalent resolved constant.
     */
    protected void replaceParent(IdentityConstant constParent)
        {
        assert !constParent.containsUnresolved();
        assert m_constParent.equals(constParent) && constParent.equals(m_constParent);

        m_constParent = constParent;
        }


    // ----- IdentityConstant methods --------------------------------------------------------------

    @Override
    public IdentityConstant getParentConstant()
        {
        return m_constParent;
        }

    public StringConstant getNameConstant()
        {
        return m_constName;
        }

    @Override
    public String getName()
        {
        return m_constName.getValue();
        }


    // ----- Constant methods ----------------------------------------------------------------------

    @Override
    public abstract Format getFormat();

    @Override
    public void forEachUnderlying(Consumer<Constant> visitor)
        {
        visitor.accept(m_constParent);
        visitor.accept(m_constName);
        }

    @Override
    public boolean containsUnresolved()
        {
        return super.containsUnresolved() || m_constName.containsUnresolved();
        }

    @Override
    protected int compareDetails(Constant that)
        {
        if (!(that instanceof NamedConstant))
            {
            return -1;
            }
        int n = this.m_constParent.compareTo(((NamedConstant) that).m_constParent);
        if (n == 0)
            {
            n = this.getName().compareTo(((NamedConstant) that).getName());
            }
        return n;
        }

    @Override
    public String getValueString()
        {
        String sParent;
        char   chSep;
        final Constant constParent = m_constParent;
        switch (constParent.getFormat())
            {
            case Module:
                sParent = ((ModuleConstant) constParent).getUnqualifiedName();
                chSep   = ':';
                break;

            case Package:
            case Class:
            case NativeClass:
                sParent = constParent.getValueString();
                chSep   = '.';
                break;

            case Property:
                sParent = ((NamedConstant) constParent).getName();
                chSep   = '#';
                break;

            case Method:
                sParent = ((MethodConstant) constParent).getName() + "(?)";
                chSep   = '#';
                break;

            case TypeParameter:
            case FormalTypeChild:
                sParent = ((NamedConstant) constParent).getName();
                chSep   = '.';
                break;

            default:
                throw new IllegalStateException("parent=" + constParent);
            }

        return sParent + chSep + m_constName.getValue();
        }


    // ----- XvmStructure methods ------------------------------------------------------------------

    @Override
    protected void registerConstants(ConstantPool pool)
        {
        m_constParent = (IdentityConstant) pool.register(m_constParent);
        m_constName   = (StringConstant)   pool.register(m_constName);
        }

    @Override
    protected void assemble(DataOutput out)
            throws IOException
        {
        out.writeByte(getFormat().ordinal());
        writePackedLong(out, m_constParent.getPosition());
        writePackedLong(out, m_constName.getPosition());
        }

    @Override
    public abstract String getDescription();


    // ----- Object methods ------------------------------------------------------------------------

    @Override
    public int hashCode()
        {
        return m_constParent.hashCode() * 17
                + m_constName.hashCode();
        }


    // ----- fields --------------------------------------------------------------------------------

    /**
     * During disassembly, this holds the index of the constant that specifies the parent structure.
     * property.
     */
    private int m_iParent;

    /**
     * During disassembly, this holds the index of the constant that specifies the name of the
     * structure identified by this constant.
     */
    private int m_iName;

    /**
     * The constant that identifies the structure which is the parent of the structure identified by
     * this constant.
     */
    private IdentityConstant m_constParent;

    /**
     * The constant that holds the name of the structure identified by this constant.
     */
    private StringConstant m_constName;
    }
