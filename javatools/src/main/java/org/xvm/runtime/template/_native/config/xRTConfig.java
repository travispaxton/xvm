package org.xvm.runtime.template._native.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.xvm.asm.ClassStructure;

import org.xvm.asm.ConstantPool;
import org.xvm.asm.MethodStructure;
import org.xvm.asm.Op;
import org.xvm.asm.constants.ModuleConstant;
import org.xvm.asm.constants.TypeConstant;
import org.xvm.asm.constants.TypedefConstant;
import org.xvm.runtime.CallChain;
import org.xvm.runtime.Container;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ServiceContext;
import org.xvm.runtime.TypeComposition;
import org.xvm.runtime.template.collections.xArray;
import org.xvm.runtime.template.collections.xListMap;
import org.xvm.runtime.template.collections.xTuple;
import org.xvm.runtime.template.text.xString;
import org.xvm.runtime.template.xService;

import static org.xvm.asm.Op.anyDeferred;

public class xRTConfig
        extends xService {

    public static xRTConfig INSTANCE;

    public xRTConfig(Container container, ClassStructure structure, boolean fInstance)
        {
        super(container, structure, false);

        if (fInstance)
            {
            INSTANCE = this;
            }
        }

    @Override
    public void initNative()
        {
        invalidateTypeInfo();

        ConstantPool    pool       = pool();
        ModuleConstant  module     = pool.ensureModuleConstant("config.xtclang.org");
        TypedefConstant configItem = pool.ensureTypedefConstant(module, "ConfigItem");

        TypeConstant typeConfig = m_typeConfigItem = configItem.getType();
        TypeConstant typeConfigItemArray = pool.ensureArrayType(typeConfig);
        m_clzConfigItemArray = f_container.resolveClass(typeConfigItemArray);

        m_typeTuple = pool.ensureTupleType(pool.typeString(), typeConfig);
        m_typeTupleArray = pool.ensureArrayType(m_typeTuple);
        m_clzTuple = f_container.resolveClass(m_typeTuple);
        m_clzTupleArray = f_container.resolveClass(m_typeTupleArray);
        }

    /**
     * Injection support method.
     */
    public ObjectHandle ensureConfig(Frame frame, ObjectHandle hOpts, Map<String, Object> map)
        {
        ObjectHandle hConfig = m_hConfig;
        if (hConfig == null)
            {
            m_hConfig = hConfig = instantiateConfig(frame, hOpts, map);
            }
        return hConfig;
        }

    protected ObjectHandle instantiateConfig(Frame frame, ObjectHandle hOpts, Map<String, Object> map)
        {
        TypeComposition clz     = getCanonicalClass();
        MethodStructure ctor    = getStructure().findConstructor();
        ServiceContext  context = f_container.createServiceContext(f_sName);

        switch (context.sendConstructRequest(frame, clz, ctor, null, new ObjectHandle[ctor.getMaxVars()], Op.A_STACK))
            {
            case Op.R_NEXT:
                return invokeCreateConfig(frame, map);

            case Op.R_CALL:
                {
                Frame frameNext = frame.m_frameNext;
                frameNext.addContinuation(frameCaller ->
                    {
                    frameCaller.pushStack(invokeCreateConfig(frameCaller, map));
                    return Op.R_NEXT;
                    });
                return new ObjectHandle.DeferredCallHandle(frameNext);
                }

            case Op.R_EXCEPTION:
                return new ObjectHandle.DeferredCallHandle(frame.m_hException);

            default:
                throw new IllegalStateException();
            }
        }

    private ObjectHandle invokeCreateConfig(Frame frame, Map<String, Object> map)
        {
        ObjectHandle    hTarget = frame.popStack();
        TypeComposition clz     = getCanonicalClass();
        MethodStructure method  = getStructure().findMethod("createConfigFromMap", 1);
        CallChain       chain   = clz.getMethodCallChain(method.getIdentityConstant().getSignature());
        ObjectHandle    hMap    = createMap(frame, ConstantPool.getCurrentPool(), map);

        switch (chain.invoke(frame, hTarget, hMap, Op.A_STACK))
            {
            case Op.R_NEXT:
                return frame.popStack();

            case Op.R_CALL:
                return new ObjectHandle.DeferredCallHandle(frame.m_frameNext);

            case Op.R_EXCEPTION:
                return new ObjectHandle.DeferredCallHandle(frame.m_hException);

            default:
                throw new IllegalStateException();
            }
        }


    private ObjectHandle createMap(Frame frame, ConstantPool pool, Map<String, Object> map)
        {
        ObjectHandle[] ahTuple = new ObjectHandle[map.size()];
        int            nIndex  = 0;

        for (Map.Entry<String, Object> entry : map.entrySet())
            {
            ObjectHandle h1 = xString.makeHandle(entry.getKey());
            ObjectHandle h2 = createConfigItem(frame, pool, entry.getValue());
            ahTuple[nIndex++] = xTuple.makeImmutableHandle(m_clzTuple, h1, h2);
            }

        return xArray.makeArrayHandle(m_clzTupleArray, ahTuple.length, ahTuple, xArray.Mutability.Constant);
//
//
//
//        TypeConstant   typeMap = pool.ensureMapType(pool.typeString(), m_typeConfigItem);
//        int            cEntry  = map.size();
//        ObjectHandle[] ahKey   = new ObjectHandle[cEntry];
//        ObjectHandle[] ahVal   = new ObjectHandle[cEntry];
//        int            nIndex  = 0;
//
//        for (Map.Entry<String, Object> entry : map.entrySet())
//            {
//            ahKey[nIndex] = xString.makeHandle(entry.getKey());
//            ahVal[nIndex]  = createConfigItem(frame, pool, entry.getValue());
//            nIndex++;
//            }
//
//        switch (xListMap.INSTANCE.constructMap(frame, typeMap, ahKey,
//                                               ahVal, anyDeferred(ahKey), anyDeferred(ahVal), Op.A_STACK))
//            {
//            case Op.R_NEXT:
//                {
//                return frame.popStack();
//                }
//
//            case Op.R_CALL:
//                {
//                return new ObjectHandle.DeferredCallHandle(frame.m_frameNext);
//                }
//
//            case Op.R_EXCEPTION:
//                return new ObjectHandle.DeferredCallHandle(frame.m_hException);
//
//            default:
//                throw new IllegalStateException();
//            }
        }

    private ObjectHandle createArray(Frame frame, ConstantPool pool, Collection<Object> col)
        {
        ObjectHandle[] ahVal = col.stream()
                .map(oValue -> createConfigItem(frame, pool, oValue))
                .toArray(ObjectHandle[]::new);

        return xArray.makeArrayHandle(m_clzConfigItemArray, ahVal.length, ahVal, xArray.Mutability.Constant);
        }

    private ObjectHandle createConfigItem(Frame frame, ConstantPool pool, Object oValue)
        {
        if (oValue instanceof String)
            {
            return xString.makeHandle((String) oValue);
            }
        else if (oValue instanceof Collection)
            {
            return createArray(frame, pool, (Collection<Object>) oValue);
            }
        else if (oValue instanceof Map)
            {
            return createMap(frame, pool, (Map<String, Object>) oValue);
            }
        throw new IllegalArgumentException("Invalid config item type: " + oValue);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The handle to the config.
     */
    private ObjectHandle m_hConfig;

    private TypeConstant m_typeConfig;

    private TypeComposition m_clzConfigItemArray;

    private TypeConstant m_typeConfigItem;

    private TypeConstant m_typeTuple;

    private TypeComposition m_clzTuple;

    private TypeConstant m_typeTupleArray;

    private TypeComposition m_clzTupleArray;
}
