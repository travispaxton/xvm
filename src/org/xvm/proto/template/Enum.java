package org.xvm.proto.template;

import org.xvm.asm.ClassStructure;
import org.xvm.asm.Component;
import org.xvm.asm.Constant;

import org.xvm.asm.PropertyStructure;
import org.xvm.asm.constants.ClassConstant;

import org.xvm.proto.Frame;
import org.xvm.proto.ObjectHandle;
import org.xvm.proto.ObjectHandle.JavaLong;
import org.xvm.proto.ObjectHeap;
import org.xvm.proto.TypeComposition;
import org.xvm.proto.TypeSet;

import java.util.ArrayList;
import java.util.List;

/**
 * A template for the base of all Enum classes
 *
 * @author gg 2017.02.27
 */
public class Enum
        extends Const
    {
    public static Enum INSTANCE;

    protected List<String> m_listNames;
    protected List<EnumHandle> m_listHandles;

    public Enum(TypeSet types, ClassStructure structure, boolean fInstance)
        {
        super(types, structure, false);

        if (fInstance)
            {
            INSTANCE = this;
            }
        }

    @Override
    public void initDeclared()
        {
        if (this == INSTANCE)
            {
            markNativeGetter("ordinal");
            markNativeGetter("name");
            }
        else if (f_struct.getFormat() == Component.Format.ENUM)
            {
            List<Component> listAll = f_struct.children();
            List<String> listNames = new ArrayList<>(listAll.size());
            List<EnumHandle> listHandles = new ArrayList<>(listAll.size());

            int cValues = 0;
            for (Component child : listAll)
                {
                if (child.getFormat() == Component.Format.ENUMVALUE)
                    {
                    listNames.add(child.getName());
                    listHandles.add(new EnumHandle(f_clazzCanonical, cValues++));
                    }
                }
            m_listNames = listNames;
            m_listHandles = listHandles;
            }
        }

    @Override
    public ObjectHandle createConstHandle(Constant constant, ObjectHeap heap)
        {
        if (constant instanceof ClassConstant)
            {
            ClassConstant constClass = (ClassConstant) constant;

            Enum template = f_struct.getFormat() == Component.Format.ENUMVALUE ?
                (Enum) getSuper() : this;

            return template.getEnumByName(constClass.getName());
            }
        return null;
        }

    @Override
    public int invokeNativeGet(Frame frame, PropertyStructure property, ObjectHandle hTarget, int iReturn)
        {
        EnumHandle hEnum = (EnumHandle) hTarget;

        switch (property.getName())
            {
            case "name":
                return frame.assignValue(iReturn,
                        xString.makeHandle(m_listNames.get((int) hEnum.getValue())));

            case "ordinal":
                return frame.assignValue(iReturn, xInt64.makeHandle(hEnum.getValue()));
            }

        return super.invokeNativeGet(frame, property, hTarget, iReturn);
        }

    @Override
    public int buildHashCode(Frame frame, ObjectHandle hTarget, int iReturn)
        {
        EnumHandle hEnum = (EnumHandle) hTarget;

        return frame.assignValue(iReturn, xInt64.makeHandle(hEnum.getValue()));
        }

    @Override
    public int buildStringValue(Frame frame, ObjectHandle hTarget, int iReturn)
        {
        EnumHandle hEnum = (EnumHandle) hTarget;

        return frame.assignValue(iReturn,
                xString.makeHandle(m_listNames.get((int) hEnum.getValue())));
        }

    // ----- helper method -----

    public EnumHandle getEnumByName(String sName)
        {
        int ix = m_listNames.indexOf(sName);
        return ix >= 0 ? m_listHandles.get(ix) : null;
        }

    public EnumHandle getEnumByOrdinal(int ix)
        {
        return ix >= 0 ? m_listHandles.get(ix) : null;
        }

    // ----- ObjectHandle -----

    public static class EnumHandle
                extends JavaLong
        {
        EnumHandle(TypeComposition clz, long lIndex)
            {
            super(clz, lIndex);
            }
        }
    }
