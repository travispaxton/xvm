package org.xvm.runtime.template._native.collections.arrays;


import java.util.HashMap;
import java.util.Map;

import org.xvm.asm.ClassStructure;

import org.xvm.asm.constants.TypeConstant;

import org.xvm.runtime.TemplateRegistry;

import org.xvm.runtime.template.collections.xArray.Mutability;


/**
 * The native RTViewFromBit base implementation.
 */
public class xRTViewFromBit
        extends xRTView
    {
    public static xRTViewFromBit INSTANCE;

    public xRTViewFromBit(TemplateRegistry templates, ClassStructure structure, boolean fInstance)
        {
        super(templates, structure);

        if (fInstance)
            {
            INSTANCE = this;
            }
        }

    @Override
    public void registerNativeTemplates()
        {
        if (this == INSTANCE)
            {
            registerNativeTemplate(new xRTViewFromBitToByte(f_templates, f_struct, true));
            }
        }

    @Override
    public void initNative()
        {
        // register native views
        Map<TypeConstant, xRTViewFromBit> mapViews = new HashMap<>();

        mapViews.put(pool().typeByte(), xRTViewFromBitToByte.INSTANCE);

        VIEWS = mapViews;
        }

    /**
     * Create an ArrayDelegate<NumType> view into the specified ArrayDelegate<Bit> source.
     *
     * @param hSource      the source (of bit type) delegate
     * @param typeElement  the numeric type to create the view for
     * @param mutability   the desired mutability
     */
    public DelegateHandle createBitViewDelegate(DelegateHandle hSource, TypeConstant typeElement,
                                                Mutability mutability)
        {
        xRTViewFromBit template = VIEWS.get(typeElement);

        if (template != null)
            {
            return template.createBitViewDelegate(hSource, typeElement, mutability);
            }
        throw new UnsupportedOperationException();
        }


    // ----- constants -----------------------------------------------------------------------------

    private static Map<TypeConstant, xRTViewFromBit> VIEWS;
    }