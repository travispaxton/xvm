package org.xvm.proto.template;

import org.xvm.proto.Frame;
import org.xvm.proto.ObjectHandle;
import org.xvm.proto.ObjectHandle.ArrayHandle;
import org.xvm.proto.ObjectHandle.ExceptionHandle;
import org.xvm.proto.ObjectHandle.JavaLong;
import org.xvm.proto.TypeComposition;
import org.xvm.proto.TypeSet;

/**
 * TODO:
 *
 * @author gg 2017.02.27
 */
public class xIntArray
        extends xArray
    {
    public static xIntArray INSTANCE;

    public xIntArray(TypeSet types)
        {
        super(types, "x:collections.IntArray", "x:collections.Array", Shape.Class);

        INSTANCE = this;
        }

    @Override
    public void initDeclared()
        {
        }

    @Override
    protected ObjectHandle extractArrayValue(ArrayHandle hArray, long lIndex)
        {
        return xInt64.makeHandle(((IntArrayHandle) hArray).m_alValue[(int) lIndex]);
        }

    @Override
    protected ExceptionHandle ensureCapacity(ArrayHandle hTarget, int cSize)
        {
        IntArrayHandle hArray = (IntArrayHandle) hTarget;

        int cCapacity = hArray.m_alValue.length;
        if (cCapacity <= cSize)
            {
            // resize (TODO: we should be much smarter here)
            cCapacity = cCapacity + Math.max(cCapacity >> 2, 16);

            long[] alNew = new long[cCapacity];
            System.arraycopy(hArray.m_alValue, 0, alNew, 0, cSize);
            hArray.m_alValue = alNew;
            }

        return null;
        }

    @Override
    protected ExceptionHandle assignArrayValue(ArrayHandle hTarget, long lIndex, ObjectHandle hValue)
        {
        ((IntArrayHandle) hTarget).m_alValue[(int) lIndex] = ((JavaLong) hValue).getValue();
        return null;
        }

    public ExceptionHandle invokePreInc(Frame frame, ObjectHandle hTarget, long lIndex, int iReturn)
        {
        IntArrayHandle hArray = (IntArrayHandle) hTarget;
        int cSize = hArray.m_cSize;

        if (lIndex < 0 || lIndex >= cSize)
            {
            return outOfRange(lIndex, cSize);
            }

        return frame.assignValue(iReturn,
                 xInt64.makeHandle(++hArray.m_alValue[(int) lIndex]));
        }

    public static IntArrayHandle makeIntArrayInstance(long cCapacity)
        {
        return new IntArrayHandle(INSTANCE.f_clazzCanonical, cCapacity);
        }

    public static class IntArrayHandle
            extends ArrayHandle
        {
        public long[] m_alValue;

        protected IntArrayHandle(TypeComposition clzArray, long cCapacity)
            {
            super(clzArray);

            m_alValue = new long[(int) cCapacity];
            }

        @Override
        public String toString()
            {
            return super.toString() + (m_fFixed ? "fixed" : "capacity=" + m_alValue.length)
                    + ", size=" + m_cSize;
            }
        }
    }
