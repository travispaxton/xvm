package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.Op;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;

import org.xvm.runtime.Utils;
import org.xvm.runtime.template.collections.xTuple.TupleHandle;

import static org.xvm.util.Handy.readPackedInt;
import static org.xvm.util.Handy.writePackedLong;


/**
 * RETURN_T rvalue-tuple ; return (a tuple of return values)
 * <p/>
 * (generated by the compiler when the current method has a multi-return, but the
 * specified return argument refers to a tuple whose field count matches the number
 * of return values and whose field types match the return types)
 */
public class Return_T
        extends Op
    {
    /**
     * Construct a RETURN_T op.
     *
     * @param nValue  the tuple value to return
     *
     * @deprecated
     */
    public Return_T(int nValue)
        {
        m_nArg = nValue;
        }

    /**
     * Construct a RETURN_T op.
     *
     * @param argT  the tuple value to return
     */
    public Return_T(Argument argT)
        {
        m_argT = argT;
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public Return_T(DataInput in, Constant[] aconst)
            throws IOException
        {
        m_nArg = readPackedInt(in);
        }

    @Override
    public void write(DataOutput out, ConstantRegistry registry)
            throws IOException
        {
        if (m_argT != null)
            {
            m_nArg = encodeArgument(m_argT, registry);
            }

        out.writeByte(OP_RETURN_T);
        writePackedLong(out, m_nArg);
        }

    @Override
    public int getOpCode()
        {
        return OP_RETURN_T;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        ObjectHandle hArg = frame.getReturnValue(m_nArg);

        if (isProperty(hArg))
            {
            ObjectHandle[] ahValue = new ObjectHandle[]{hArg};
            Frame.Continuation stepNext =
                frameCaller -> frameCaller.returnTuple((TupleHandle) ahValue[0]);

            return new Utils.GetArgument(ahValue, stepNext).doNext(frame);
            }

        return frame.returnTuple((TupleHandle) hArg);
        }

    @Override
    public void registerConstants(ConstantRegistry registry)
        {
        registerArgument(m_argT, registry);
        }

    private int m_nArg;

    private Argument m_argT;
    }
