package org.xvm.compiler.ast;


import java.math.BigDecimal;

import org.xvm.asm.Constant;
import org.xvm.asm.Constant.Format;
import org.xvm.asm.ConstantPool;
import org.xvm.asm.Constants.Access;
import org.xvm.asm.MethodStructure.Code;
import org.xvm.asm.Op.Argument;

import org.xvm.asm.constants.ClassConstant;
import org.xvm.asm.constants.Float16Constant;
import org.xvm.asm.constants.ImmutableTypeConstant;
import org.xvm.asm.constants.TypeConstant;

import org.xvm.compiler.Compiler;
import org.xvm.compiler.ErrorListener;
import org.xvm.compiler.Token;
import org.xvm.compiler.Token.Id;

import org.xvm.type.Decimal32;
import org.xvm.type.Decimal64;
import org.xvm.type.Decimal128;

import org.xvm.util.Handy;
import org.xvm.util.PackedInteger;
import org.xvm.util.Severity;


/**
 * A literal expression specifies a literal value.
 */
public class LiteralExpression
        extends Expression
    {
    // ----- constructors --------------------------------------------------------------------------

    public LiteralExpression(Token literal)
        {
        this.literal = literal;
        }


    // ----- accessors -----------------------------------------------------------------------------

    /**
     * @return true iff the LiteralExpression is the result of an empty T0D0 expression
     */
    public boolean isTODO()
        {
        return literal.getId() == Id.TODO;
        }

    private boolean isIntInRange(long lLower, long lUpper)
        {
        if (literal.getId() != Id.LIT_INT)
            {
            return false;
            }

        PackedInteger piVal = (PackedInteger) literal.getValue();
        return !piVal.isBig() && piVal.getLong() >= lLower && piVal.getLong() <= lUpper;
        }

    @Override
    public long getStartPosition()
        {
        return literal.getStartPosition();
        }

    @Override
    public long getEndPosition()
        {
        return literal.getEndPosition();
        }


    // ----- compilation ---------------------------------------------------------------------------

    @Override
    public boolean isConstant()
        {
        return true;
        }

    @Override
    public Constant toConstant()
        {
        // the LiteralExpression produces literal constants:
        // - Char
        // - String
        // - IntLiteral
        // - FPLiteral
        // other types are possible to obtain because there are conversion methods on the literal
        // types that provide support for other constant types
        ConstantPool pool = pool();
        switch (literal.getId())
            {
            case LIT_CHAR:
                return pool.ensureCharConstant(((Character) literal.getValue()).charValue());

            case TODO:              // the T0D0 keyword has a String text for the token's value
            case LIT_STRING:
                return pool.ensureStringConstant((String) literal.getValue());

            case LIT_INT:
                return pool.ensureLiteralConstant(Format.IntLiteral, literal.getString(getSource()));

            case LIT_DEC:
            case LIT_BIN:
                return pool.ensureLiteralConstant(Format.FPLiteral, literal.getString(getSource()));

            default:
                throw new IllegalStateException(literal.getId().name() + "=" + literal.getValue());
            }
        }

    @Override
    public TypeConstant getImplicitType()
        {
        ConstantPool pool = pool();
        switch (literal.getId())
            {
            case LIT_CHAR:
                return pool.typeChar();

            case TODO:              // the T0D0 keyword has a String text for the token's value
            case LIT_STRING:
                return pool.typeString();

            case LIT_INT:
                return pool.typeIntLiteral();

            case LIT_DEC:
            case LIT_BIN:
                return pool.typeFPLiteral();

            default:
                throw new IllegalStateException(literal.getId().name() + "=" + literal.getValue());
            }
        }

    @Override
    public boolean isAssignableTo(TypeConstant typeThat)
        {
        if (typeThat instanceof ImmutableTypeConstant)
            {
            // all of the literal expression arguments are "const" objects, so immutable is OK
            return isAssignableTo(typeThat.getUnderlyingType());
            }

        // TODO allow UncheckedInt annotation, but only for int/uint types

        if (typeThat.isSingleDefiningConstant()
                && typeThat.getDefiningConstant() instanceof ClassConstant
                && ((ClassConstant) typeThat.getDefiningConstant()).getModuleConstant().isEcstasyModule()
                && typeThat.getAccess() == Access.PUBLIC)
            {
            Id            id    = literal.getId();
            PackedInteger piVal;
            String        sName = ((ClassConstant) typeThat.getDefiningConstant()).getPathString();
            switch (sName)
                {
                // all of the literal types are const objects
                case "Object":
                case "Const":
                case "Orderable":
                case "collections.Hashable":
                    return true;

                case "Char":
                case "Sequential":                  // char implements Sequential
                    return id == Id.LIT_CHAR;

                case "Sequence":
                    if (typeThat.isParamsSpecified() && !(typeThat.isParamsSpecified(1)
                            && typeThat.getParamTypes().get(0).isA(pool().typeChar())))
                        {
                        return false;
                        }
                    // fall through
                case "String":
                    return id == Id.LIT_CHAR || id == Id.LIT_STRING;

                case "IntLiteral":
                case "Bit":
                case "Nibble":
                case "Int8":
                case "Int16":
                case "Int32":
                case "Int64":
                case "Int128":
                case "VarInt":
                case "UInt8":
                case "UInt16":
                case "UInt32":
                case "UInt64":
                case "UInt128":
                case "VarUInt":
                case "annotations.UncheckedInt":
                    return id == Id.LIT_INT;

                case "Dec32":
                case "Dec64":
                case "Dec128":
                case "VarDec":
                    return id == Id.LIT_INT || id == Id.LIT_DEC;

                case "FPLiteral":
                case "Float16":
                case "Float32":
                case "Float64":
                case "Float128":
                case "VarFloat":
                    return id == Id.LIT_INT || id == Id.LIT_DEC || id == Id.LIT_BIN;

                case "Number":
                case "IntNumber":
                case "UIntNumber":
                case "FPNumber":
                case "BinaryFPNumber":
                case "DecimalFPNumber":
                    // these types are ambiguous; while it's possible to covert to something that
                    // implements the specified interface, the problem is that it's possible to
                    // convert to _several_ different things that implement the same interface!
                    // (we could let it fall through and let the default impl figure it out, but we
                    // already know the answer)
                    return false;
                }
            }

        return super.isAssignableTo(typeThat);
        }

    @Override
    public Argument generateConstant(TypeConstant constType, ErrorListener errs)
        {
        if (constType.isSingleDefiningConstant()
                && constType.getDefiningConstant() instanceof ClassConstant
                && ((ClassConstant) constType.getDefiningConstant()).getModuleConstant().isEcstasyModule()
                && (!constType.isAccessSpecified() || constType.getAccess() == Access.PUBLIC))
            {
            ConstantPool  pool     = pool();
            ClassConstant constClz = (ClassConstant) constType.getDefiningConstant();
            String        sName    = constClz.getPathString();

            switch (sName)
                {
                // these are all of the super-classes and interfaces that could be represented by
                // a literal expression
                case "Object":
                case "Const":
                case "Orderable":
                case "collections.Hashable":
                    return toConstant();

                case "Sequential":
                case "Char":
                    // note: char is the only literal type that implements sequential
                    if (literal.getId() == Id.LIT_CHAR)
                        {
                        return toConstant();
                        }
                    break;

                case "Sequence":
                    if (constType.isParamsSpecified() && !(constType.isParamsSpecified(1)
                            && constType.getParamTypes().get(0).isA(pool().typeChar())))
                        {
                        break;
                        }
                    // fall through
                case "String":
                    switch (literal.getId())
                        {
                        case LIT_CHAR:
                        case LIT_STRING:
                            return pool.ensureStringConstant(literal.getValue().toString());
                        }
                    break;

                case "IntLiteral":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        return toConstant();
                        }
                    break;

                case "FPLiteral":
                    switch (literal.getId())
                        {
                        case LIT_INT:
                        case LIT_DEC:
                        case LIT_BIN:
                            return toConstant();
                        }
                    break;

                case "Bit":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        int n;
                        if (isIntInRange(0, 1))
                            {
                            n = ((PackedInteger) literal.getValue()).getInt();
                            }
                        else
                            {
                            n = 0;
                            log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                    sName, literal.getString(getSource()));
                            }
                        return pool.ensureBitConstant(n);
                        }
                    break;

                case "Nibble":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        int n;
                        if (isIntInRange(0x0, 0xF))
                            {
                            n = ((PackedInteger) literal.getValue()).getInt();
                            }
                        else
                            {
                            n = 0;
                            log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                    sName, literal.getString(getSource()));
                            }
                        return pool.ensureNibbleConstant(n);
                        }
                    break;

                case "Int8":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        PackedInteger piVal = (PackedInteger) literal.getValue();
                        if (!isIntInRange(Byte.MIN_VALUE, Byte.MAX_VALUE))
                            {
                            log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                    sName, literal.getString(getSource()));
                            piVal = PackedInteger.ZERO;
                            }
                        return pool.ensureInt8Constant(piVal.getInt());
                        }
                    break;

                case "Int16":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        PackedInteger piVal = (PackedInteger) literal.getValue();
                        if (!isIntInRange(Short.MIN_VALUE, Short.MAX_VALUE))
                            {
                            log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                    sName, literal.getString(getSource()));
                            piVal = PackedInteger.ZERO;
                            }
                        return pool.ensureIntConstant(piVal, Format.Int16);
                        }
                    break;

                case "Int32":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        PackedInteger piVal = (PackedInteger) literal.getValue();
                        if (!isIntInRange(Integer.MIN_VALUE, Integer.MAX_VALUE))
                            {
                            log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                    sName, literal.getString(getSource()));
                            piVal = PackedInteger.ZERO;
                            }
                        return pool.ensureIntConstant(piVal, Format.Int32);
                        }
                    break;

                case "Int64":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        PackedInteger piVal = (PackedInteger) literal.getValue();
                        if (!isIntInRange(Long.MIN_VALUE, Long.MAX_VALUE))
                            {
                            log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                    sName, literal.getString(getSource()));
                            piVal = PackedInteger.ZERO;
                            }
                        return pool.ensureIntConstant(piVal);
                        }
                    break;

                case "Int128":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        PackedInteger piVal = (PackedInteger) literal.getValue();
                        if (piVal.isBig() && piVal.getSignedByteSize() > 16)
                            {
                            log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                    sName, literal.getString(getSource()));
                            piVal = PackedInteger.ZERO;
                            }
                        return pool.ensureIntConstant(piVal, Format.Int128);
                        }
                    break;

                case "VarInt":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        return pool.ensureIntConstant(
                                (PackedInteger) literal.getValue(), Format.VarInt);
                        }
                    break;

                case "UInt8":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        PackedInteger piVal = (PackedInteger) literal.getValue();
                        if (!isIntInRange(0x00, 0xFF))
                            {
                            log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                    sName, literal.getString(getSource()));
                            piVal = PackedInteger.ZERO;
                            }
                        return pool.ensureUInt8Constant(piVal.getInt());
                        }
                    break;

                case "UInt16":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        PackedInteger piVal = (PackedInteger) literal.getValue();
                        if (!isIntInRange(0x0000, 0xFFFF))
                            {
                            log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                    sName, literal.getString(getSource()));
                            piVal = PackedInteger.ZERO;
                            }
                        return pool.ensureIntConstant(piVal, Format.UInt16);
                        }
                    break;

                case "UInt32":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        PackedInteger piVal = (PackedInteger) literal.getValue();
                        if (!isIntInRange(0x00000000L, 0xFFFFFFFFL))
                            {
                            log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                    sName, literal.getString(getSource()));
                            piVal = PackedInteger.ZERO;
                            }
                        return pool.ensureIntConstant(piVal, Format.UInt32);
                        }
                    break;

                case "UInt64":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        PackedInteger piVal = (PackedInteger) literal.getValue();
                        if (piVal.isNegative() || piVal.isBig() && piVal.getUnsignedByteSize() > 8)
                            {
                            log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                    sName, literal.getString(getSource()));
                            piVal = PackedInteger.ZERO;
                            }
                        return pool.ensureIntConstant(piVal, Format.UInt64);
                        }

                case "UInt128":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        PackedInteger piVal = (PackedInteger) literal.getValue();
                        if (piVal.isNegative() || piVal.isBig() && piVal.getUnsignedByteSize() > 16)
                            {
                            log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                    sName, literal.getString(getSource()));
                            piVal = PackedInteger.ZERO;
                            }
                        return pool.ensureIntConstant(piVal, Format.UInt128);
                        }

                case "VarUInt":
                    if (literal.getId() == Id.LIT_INT)
                        {
                        PackedInteger piVal = (PackedInteger) literal.getValue();
                        if (piVal.isNegative())
                            {
                            log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                    sName, literal.getString(getSource()));
                            piVal = PackedInteger.ZERO;
                            }
                        return pool.ensureIntConstant(piVal, Format.VarUInt);
                        }

                case "Dec32":
                    switch (literal.getId())
                        {
                        case LIT_INT:
                        case LIT_DEC:
                            BigDecimal bigdec = literal.getId() == Id.LIT_INT
                                    ? new BigDecimal(((PackedInteger) literal.getValue()).getBigInteger())
                                    : (BigDecimal) literal.getValue();
                            try
                                {
                                return pool.ensureDecimalConstant(new Decimal32(bigdec));
                                }
                            catch (ArithmeticException e) {}
                        }
                    break;

                case "Dec64":
                    switch (literal.getId())
                        {
                        case LIT_INT:
                        case LIT_DEC:
                            BigDecimal bigdec = literal.getId() == Id.LIT_INT
                                    ? new BigDecimal(((PackedInteger) literal.getValue()).getBigInteger())
                                    : (BigDecimal) literal.getValue();
                            try
                                {
                                return pool.ensureDecimalConstant(new Decimal64(bigdec));
                                }
                            catch (ArithmeticException e) {}
                        }
                    break;

                case "Dec128":
                    switch (literal.getId())
                        {
                        case LIT_INT:
                        case LIT_DEC:
                            BigDecimal bigdec = literal.getId() == Id.LIT_INT
                                    ? new BigDecimal(((PackedInteger) literal.getValue()).getBigInteger())
                                    : (BigDecimal) literal.getValue();
                            try
                                {
                                return pool.ensureDecimalConstant(new Decimal128(bigdec));
                                }
                            catch (ArithmeticException e) {}
                        }
                    break;

                case "VarDec":
                    switch (literal.getId())
                        {
                        case LIT_INT:
                        case LIT_DEC:
                            BigDecimal bigdec = literal.getId() == Id.LIT_INT
                                    ? new BigDecimal(((PackedInteger) literal.getValue()).getBigInteger())
                                    : (BigDecimal) literal.getValue();
                            // TODO - support variable-length decimal
                            throw new UnsupportedOperationException("var-len decimal not implemented");
                        }
                    break;

                case "Float16":
                    switch (literal.getId())
                        {
                        case LIT_INT:
                        case LIT_DEC:
                        case LIT_BIN: // TODO add support for *purposeful* NaN/infinity
                            float   flVal = 0;
                            boolean fErr  = false;
                            try
                                {
                                flVal = Float.parseFloat(literal.getValue().toString());
                                }
                            catch (NumberFormatException e)
                                {
                                fErr = true;
                                }
                            // convert to/from 16-bit to test for overflow/underflow
                            if (fErr || !Float.isFinite(flVal) || !Float.isFinite(
                                    Float16Constant.toFloat(Float16Constant.toHalf(flVal))))
                                {
                                log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                        sName, literal.getString(getSource()));
                                flVal = 0;
                                }
                            return pool.ensureFloat16Constant(flVal);
                        }
                    break;

                case "Float32":
                    switch (literal.getId())
                        {
                        case LIT_INT:
                        case LIT_DEC:
                        case LIT_BIN: // TODO add support for *purposeful* NaN/infinity
                            float   flVal = 0;
                            boolean fErr  = false;
                            try
                                {
                                flVal = Float.parseFloat(literal.getValue().toString());
                                }
                            catch (NumberFormatException e)
                                {
                                fErr = true;
                                }
                            if (fErr || !Float.isFinite(flVal))
                                {
                                log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                        sName, literal.getString(getSource()));
                                flVal = 0;
                                }
                            return pool.ensureFloat32Constant(flVal);
                        }
                    break;

                case "Float64":
                    switch (literal.getId())
                        {
                        case LIT_INT:
                        case LIT_DEC:
                        case LIT_BIN: // TODO add support for *purposeful* NaN/infinity
                            double  flVal = 0;
                            boolean fErr  = false;
                            try
                                {
                                flVal = Double.parseDouble(literal.getValue().toString());
                                }
                            catch (NumberFormatException e)
                                {
                                fErr = true;
                                }
                            if (fErr || !Double.isFinite(flVal))
                                {
                                log(errs, Severity.ERROR, Compiler.VALUE_OUT_OF_RANGE,
                                        sName, literal.getString(getSource()));
                                flVal = 0;
                                }
                            return pool.ensureFloat64Constant(flVal);
                        }
                    break;

                case "Float128":
                    // TODO - support 128-bit float
                    throw new UnsupportedOperationException("128-bit binary floating point not implemented");

                case "VarFloat":
                    // TODO - support var-len float
                    throw new UnsupportedOperationException("var-len binary floating point not implemented");
                }
            }

        return super.generateConstant(constType, errs);
        }

    @Override
    public Argument generateArgument(Code code, TypeConstant constType, boolean fTupleOk, ErrorListener errs)
        {
        if (constType.isSingleDefiningConstant()
                && constType.getDefiningConstant() instanceof ClassConstant
                && ((ClassConstant) constType.getDefiningConstant()).getModuleConstant().isEcstasyModule()
                && constType.getAccess() == Access.PUBLIC)
            {
            String sName = ((ClassConstant) constType.getDefiningConstant()).getPathString();
            switch (sName)
                {
                case "Sequence":
                    if (constType.isParamsSpecified() && !(constType.isParamsSpecified(1)
                            && constType.getParamTypes().get(0).isA(pool().typeChar())))
                        {
                        break;
                        }
                    // fall through
                case "Object":
                case "Const":
                case "Orderable":
                case "collections.Hashable":
                case "Char":
                case "Sequential":                  // char implements Sequential
                case "String":
                case "IntLiteral":
                case "Bit":
                case "Nibble":
                case "Int8":
                case "Int16":
                case "Int32":
                case "Int64":
                case "Int128":
                case "VarInt":
                case "UInt8":
                case "UInt16":
                case "UInt32":
                case "UInt64":
                case "UInt128":
                case "VarUInt":
                case "Dec32":
                case "Dec64":
                case "Dec128":
                case "VarDec":
                case "FPLiteral":
                case "Float16":
                case "Float32":
                case "Float64":
                case "Float128":
                case "VarFloat":
                    return generateConstant(constType, errs);
                }
            }

        return super.generateArgument(code, constType, fTupleOk, errs);
        }


    // ----- debugging assistance ------------------------------------------------------------------

    @Override
    public String toString()
        {
        switch (literal.getId())
            {
            case LIT_INT:
            case LIT_DEC:
            case LIT_BIN:
                return String.valueOf(literal.getValue());

            case LIT_CHAR:
                 return Handy.quotedChar((Character) literal.getValue());

            case LIT_STRING:
                 return Handy.quotedString(String.valueOf(literal.getValue()));

            default:
                throw new IllegalStateException(literal.getId().name() + "=" + literal.getValue());
            }
        }

    @Override
    public String getDumpDesc()
        {
        return toString();
        }


    // ----- fields --------------------------------------------------------------------------------

    protected Token literal;
    }
