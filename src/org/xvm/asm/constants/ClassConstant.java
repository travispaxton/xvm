package org.xvm.asm.constants;


import java.io.DataInput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;


/**
 * Represent a Class constant, which identifies a specific class structure.
 */
public class ClassConstant
        extends NamedConstant
    {
    // ----- constructors --------------------------------------------------------------------------

    /**
     * Constructor used for deserialization.
     *
     * @param pool    the ConstantPool that will contain this Constant
     * @param format  the format of the Constant in the stream
     * @param in      the DataInput stream to read the Constant value from
     *
     * @throws IOException  if an issue occurs reading the Constant value
     */
    public ClassConstant(ConstantPool pool, Format format, DataInput in)
            throws IOException
        {
        super(pool, format, in);
        }

    /**
     * Construct a constant whose value is a class identifier.
     *
     * @param pool         the ConstantPool that will contain this Constant
     * @param constParent  the module, package, class, or method that contains this class
     * @param sName        the unqualified class name
     */
    public ClassConstant(ConstantPool pool, IdentityConstant constParent, String sName)
        {
        super(pool, constParent, sName);

        if (    !( constParent.getFormat() == Format.Module
                || constParent.getFormat() == Format.Package
                || constParent.getFormat() == Format.Class
                || constParent.getFormat() == Format.Method ))
            {
            throw new IllegalArgumentException("parent module, package, class, or method required");
            }
        }


    // ----- ClassConstant methods -----------------------------------------------------------------

    /**
     * @return return the "outermost" class that represents an auto-narrowing base
     */
    public ClassConstant getOutermost()
        {
        ClassConstant    outermost = this;
        IdentityConstant parent    = outermost.getParentConstant();
        while (true)
            {
            switch (parent.getFormat())
                {
                case Class:
                    outermost = (ClassConstant) parent;
                    break;

                case Property:
                    // ignored (we'll use its parent)
                    break;

                // methods, packages, modules all "terminate" this search
                default:
                    return outermost;
                }

            parent = parent.getParentConstant();
            }
        }

    public int getDepthFromOutermost()
        {
        int cLevelsDown = 0;
        ClassConstant    outermost = this;
        IdentityConstant parent    = outermost.getParentConstant();
        while (true)
            {
            switch (parent.getFormat())
                {
                case Class:
                    ++cLevelsDown;
                    outermost = (ClassConstant) parent;
                    break;

                case Property:
                    ++cLevelsDown;
                    break;

                // methods, packages, modules all mean we've passed the outer-most
                default:
                    return cLevelsDown;
                }

            parent = parent.getParentConstant();
            }
        }

    /**
     * Calculate an auto-narrowing constant that describes a "relative path" from this
     * class constant to the specified one.
     *
     * @param constThatClass  the class constant to calculate the "path" for
     *
     * @return a PseudoConstant representing the path or the specified constant itself if no path
     *         can be found
     */
    public Constant calculateAutoNarrowingConstant(ClassConstant constThatClass)
        {
        ClassConstant constThisClass = this;

        // if "this:class" is the same as constId, then use ThisClassConstant(constId)
        if (constThisClass.equals(constThatClass))
            {
            return new ThisClassConstant(getConstantPool(), constThisClass);
            }

        // get the "outermost class" for both "this:class" and constId
        ClassConstant constThisOutermost = constThisClass.getOutermost();
        ClassConstant constThatOutermost = constThatClass.getOutermost();
        if (constThisOutermost.equals(constThatOutermost))
            {
            // the two classes are related, so figure out how to describe "that" in relation
            // to "this"
            ConstantPool     pool       = getConstantPool();
            PseudoConstant   constPath  = new ThisClassConstant(pool, constThisClass);
            IdentityConstant constThis  = constThisClass;
            IdentityConstant constThat  = constThatClass;
            int              cThisDepth = constThisClass.getDepthFromOutermost();
            int              cThatDepth = constThatClass.getDepthFromOutermost();
            int              cReDescend = 0;
            while (cThisDepth > cThatDepth)
                {
                constPath = new ParentClassConstant(pool, constPath);
                constThis = constThis.getParentConstant();
                --cThisDepth;
                }
            while (cThatDepth > cThisDepth)
                {
                ++cReDescend;
                constThat = constThat.getParentConstant();
                --cThatDepth;
                }
            while (!constThis.equals(constThat))
                {
                assert cThisDepth == cThatDepth && cThisDepth >= 0;

                ++cReDescend;
                constPath = new ParentClassConstant(pool, constPath);

                constThis = constThis.getParentConstant();
                constThat = constThat.getParentConstant();
                --cThisDepth;
                --cThatDepth;
                }

            return redescend(constPath, constThatClass, cReDescend);
            }

        return constThatClass;
        }


    /**
     * Recursively build onto the passed path to navigate the specified number of levels down to the
     * specified child.
     *
     * @param constPath   the path, thus far
     * @param constChild  the child to navigate to
     * @param cLevels     the number of levels down that the child is
     *
     * @return a PseudoConstant that represents the navigation down to the child
     */
    private PseudoConstant redescend(PseudoConstant constPath, IdentityConstant constChild, int cLevels)
        {
        if (cLevels == 0)
            {
            return constPath;
            }

        if (cLevels > 1)
            {
            constPath = redescend(constPath, constChild.getParentConstant(), cLevels-1);
            }

        return new ChildClassConstant(getConstantPool(), constPath, constChild.getName());
        }


    // ----- Constant methods ----------------------------------------------------------------------

    @Override
    public Format getFormat()
        {
        return Format.Class;
        }

    @Override
    public boolean isClass()
        {
        return true;
        }


    // ----- XvmStructure methods ------------------------------------------------------------------

    @Override
    public String getDescription()
        {
        Constant constParent = getNamespace();
        while (constParent instanceof ClassConstant)
            {
            constParent = ((ClassConstant) constParent).getNamespace();
            }

        return "class=" + getValueString() + ", " + constParent.getDescription();
        }
    }
