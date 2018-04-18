package org.xvm.runtime.template;


import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import org.xvm.asm.ClassStructure;
import org.xvm.asm.Constant;

import org.xvm.asm.constants.ModuleConstant;

import org.xvm.runtime.ClassTemplate;
import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.TypeComposition;
import org.xvm.runtime.TemplateRegistry;


/**
 * TODO:
 */
public class xModule
        extends ClassTemplate
    {
    public static xModule INSTANCE;

    public xModule(TemplateRegistry templates, ClassStructure structure, boolean fInstance)
        {
        super(templates, structure);

        if (fInstance)
            {
            INSTANCE = this;
            }
        }

    @Override
    public void initDeclared()
        {
        }

    @Override
    public ObjectHandle createConstHandle(Frame frame, Constant constant)
        {
        if (constant instanceof ModuleConstant)
            {
            ModuleConstant constModule = (ModuleConstant) constant;

            return f_mapModules.computeIfAbsent(constModule.getName(),
                    sName -> new ModuleHandle(getCanonicalClass(), sName));
            }
        return null;
        }

    public static class ModuleHandle extends ObjectHandle
        {
        String m_sName;

        protected ModuleHandle(TypeComposition clazz, String sName)
            {
            super(clazz);
            m_sName = sName;
            }
        }

    private final Map<String, ModuleHandle> f_mapModules = new ConcurrentHashMap<>(3);
    }
