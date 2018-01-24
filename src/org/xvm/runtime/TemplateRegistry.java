package org.xvm.runtime;


import java.io.File;

import java.net.URL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import org.xvm.asm.ClassStructure;
import org.xvm.asm.Component;
import org.xvm.asm.ConstantPool;
import org.xvm.asm.GenericTypeResolver;
import org.xvm.asm.ModuleStructure;

import org.xvm.asm.constants.ClassConstant;
import org.xvm.asm.constants.IdentityConstant;
import org.xvm.asm.constants.TypeConstant;

import org.xvm.runtime.template.xConst;
import org.xvm.runtime.template.xEnum;
import org.xvm.runtime.template.xObject;
import org.xvm.runtime.template.xService;


/**
 * The type registry.
 */
public class TemplateRegistry
    {
    public final Container f_container;
    public final Adapter f_adapter;

    // cache - TypeConstant by name (only for core classes)
    private final Map<String, TypeConstant> f_mapTypesByName = new ConcurrentHashMap<>();

    // cache - ClassTemplates by type
    private final Map<TypeConstant, ClassTemplate> f_mapTemplatesByType = new ConcurrentHashMap<>();

    public final static TypeConstant[] VOID = ConstantPool.NO_TYPES;

    TemplateRegistry(Container container)
        {
        f_container = container;
        f_adapter = container.f_adapter;
        }

    protected void loadNativeTemplates()
        {
        Class clzObject = xObject.class;
        URL url = clzObject.getProtectionDomain().getCodeSource().getLocation();
        String sRoot = url.getFile();

        File dirNative = new File(sRoot, "org/xvm/runtime/template");
        Map<String, Class> mapTemplateClasses = new HashMap<>();
        scanNativeDirectory(dirNative, "", mapTemplateClasses);

        ConstantPool pool = f_container.f_pool;
        ModuleStructure module = f_container.f_module;

        // we need a number of INSTANCE static variables to be set up right away
        // (they are used by the ClassTemplate constructor)
        storeNativeTemplate(new xObject(this, (ClassStructure) pool.clzObject().getComponent(), true));
        storeNativeTemplate(new xEnum(this, (ClassStructure) pool.clzEnum().getComponent(), true));
        storeNativeTemplate(new xConst(this, (ClassStructure) pool.clzConst().getComponent(), true));
        storeNativeTemplate(new xService(this, (ClassStructure) pool.clzService().getComponent(), true));

        for (Map.Entry<String, Class> entry : mapTemplateClasses.entrySet())
            {
            ClassStructure structClass = (ClassStructure) module.getChildByPath(entry.getKey());
            if (structClass == null)
                {
                // this is a native class for a composite type;
                // it will be declared by the corresponding "primitive"
                // (see xArray.initDeclared() for an example)
                continue;
                }

            if (f_mapTemplatesByType.containsKey(structClass.getCanonicalType()))
                {
                // already loaded - one of the "base" ones
                continue;
                }

            Class<ClassTemplate> clz = entry.getValue();

            try
                {
                storeNativeTemplate(clz.getConstructor(
                    TemplateRegistry.class, ClassStructure.class, Boolean.TYPE).
                    newInstance(this, structClass, Boolean.TRUE));
                }
            catch (Exception e)
                {
                throw new RuntimeException("Constructor failed for " + clz.getName(), e);
                }
            }

        // clone the map since the loop below can add to it
        Set<ClassTemplate> setTemplates = new HashSet<>(f_mapTemplatesByType.values());

        for (ClassTemplate template : setTemplates)
            {
            if (template.f_sName.startsWith("TestApp"))
                {
                // TODO: remove - test classes
                continue;
                }

            template.initDeclared();
            }

        // TODO: remove - test classes
        getTemplate("TestApp.TestService").initDeclared();
        getTemplate("TestApp.TestClass2").initDeclared();
        getTemplate("TestApp.TestClass").initDeclared();
        getTemplate("TestApp").initDeclared();
        }

    // sPackage is either empty or ends with a dot
    private void scanNativeDirectory(File dirNative, String sPackage, Map<String, Class> mapTemplateClasses)
        {
        for (String sName : dirNative.list())
            {
            if (sName.endsWith(".class"))
                {
                if (sName.startsWith("x") && !sName.contains("$"))
                    {
                    String sSimpleName = sName.substring(1, sName.length() - 6);
                    String sQualifiedName = sPackage + sSimpleName;
                    String sClass = "org.xvm.runtime.template." + sPackage + "x" + sSimpleName;

                    try
                        {
                        mapTemplateClasses.put(sQualifiedName, Class.forName(sClass));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new IllegalStateException("Cannot load " + sClass, e);
                        }
                    }
                }
            else
                {
                File dir = new File(dirNative, sName);
                if (dir.isDirectory())
                    {
                    scanNativeDirectory(dir, sPackage.isEmpty() ? sName + '.' : sPackage + sName + '.',
                        mapTemplateClasses);
                    }
                }
            }
        }

    protected void storeNativeTemplate(ClassTemplate template)
        {
        TypeConstant typeCanonical = template.getCanonicalType();

        if (typeCanonical.isParamsSpecified())
            {
            // register just a naked underlying type
            typeCanonical = typeCanonical.getUnderlyingType();
            }
        registerNativeTemplate(typeCanonical, template);
        }

    public void registerNativeTemplate(TypeConstant type, ClassTemplate template)
        {
        f_mapTemplatesByType.putIfAbsent(type, template);
        }

    // ----- templates and structures -----

    public ClassStructure getClassStructure(String sName)
        {
        // TODO: plug in module repositories
        try
            {
            return (ClassStructure) f_container.f_module.getChildByPath(sName);
            }
        catch (ClassCastException e)
            {
            throw new IllegalArgumentException("Not a class: " + sName);
            }
        }

    public TypeConstant getTypeConstant(String sName)
        {
        try
            {
            return f_mapTypesByName.computeIfAbsent(sName, s ->
                getClassStructure(s).getIdentityConstant().asTypeConstant());
            }
        catch (NullPointerException e)
            {
            throw new IllegalArgumentException("Missing constant: " + sName);
            }
        }

    public ClassConstant getClassConstant(String sName)
        {
        try
            {
            return (ClassConstant) getClassStructure(sName).getIdentityConstant();
            }
        catch (ClassCastException e)
            {
            throw new IllegalArgumentException("Not a class: " + sName);
            }
        }

    public ClassTemplate getTemplate(String sName)
        {
        // for core classes only
        TypeConstant type = getTypeConstant(sName);
        return type == null ? null : getTemplate(type);
        }

    // obtain a ClassTemplate for the specified type
    public ClassTemplate getTemplate(TypeConstant typeActual)
        {
        ClassTemplate template = f_mapTemplatesByType.get(typeActual);
        if (template == null)
            {
            if (typeActual.isSingleDefiningConstant())
                {
                ClassConstant constClz = (ClassConstant) typeActual.getDefiningConstant();
                template = getTemplate(constClz);
                f_mapTemplatesByType.put(typeActual, template);
                }
            else
                {
                throw new UnsupportedOperationException();
                }
            }
        return template;
        }

    public ClassTemplate getTemplate(IdentityConstant constClass)
        {
        return f_mapTemplatesByType.computeIfAbsent(constClass.asTypeConstant(), type ->
            {
            Component struct = constClass.getComponent();
            ClassStructure structClass = (ClassStructure) struct;
            if (structClass == null)
                {
                throw new RuntimeException("Missing class structure: " + constClass);
                }

            ClassTemplate template;
            switch (structClass.getFormat())
                {
                case ENUMVALUE:
                    // no need to call initDeclared() for the values
                    template = new xEnum(this, structClass, false);
                    break;

                case ENUM:
                    template = new xEnum(this, structClass, false);
                    template.initDeclared();
                    break;

                case CLASS:
                case INTERFACE:
                case MIXIN:
                    template = new xObject(this, structClass, false);
                    break;

                case SERVICE:
                    template = new xService(this, structClass, false);
                    break;

                case CONST:
                    template = new xConst(this, structClass, false);
                    break;

                default:
                    throw new UnsupportedOperationException("Format is not supported: " + structClass);
                }
            return template;
            });
        }

    // ----- TypeCompositions -----

    // ensure a TypeComposition for a type referred by a TypeConstant in the ConstantPool
    public TypeComposition resolveClass(int nTypeConstId, GenericTypeResolver resolver)
        {
        TypeConstant type = (TypeConstant)
                f_container.f_pool.getConstant(nTypeConstId); // must exist

        return resolveClass(type.resolveGenerics(resolver));
        }

    // produce a TypeComposition based on the specified TypeConstant
    // using the specified actual type parameters
    public TypeComposition resolveClass(TypeConstant typeActual)
        {
        return typeActual.getOpSupport(this).ensureClass(typeActual, typeActual);
        }
    }
