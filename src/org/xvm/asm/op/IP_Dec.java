package org.xvm.asm.op;


import java.io.DataInput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.OpInPlace;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;


/**
 * IP_DEC lvalue-target ; in-place decrement; no result
 */
public class IP_Dec
        extends OpInPlace
    {
    /**
     * Construct an IP_DEC op for the passed arguments.
     *
     * @param argTarget  the target Argument
     */
    public IP_Dec(Argument argTarget)
        {
        super(argTarget);
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public IP_Dec(DataInput in, Constant[] aconst)
            throws IOException
        {
        super(in, aconst);
        }

    @Override
    public int getOpCode()
        {
        return OP_IP_DEC;
        }

    @Override
    protected boolean isAssignOp()
        {
        return false;
        }

    @Override
    protected int completeWithRegister(Frame frame, ObjectHandle hTarget)
        {
        return hTarget.getOpSupport().invokePrev(frame, hTarget, m_nTarget);
        }

    @Override
    protected int completeWithProperty(Frame frame, String sProperty)
        {
        ObjectHandle hTarget = frame.getThis();

        return hTarget.getTemplate().invokePostDec(
            frame, hTarget, sProperty, Frame.RET_UNUSED);
        }
    }