package org.xvm.compiler.ast;


import org.xvm.asm.Constant.Format;
import org.xvm.asm.ConstantPool;

import org.xvm.asm.Op.Argument;

import org.xvm.asm.constants.ConditionalConstant;
import org.xvm.asm.constants.LiteralConstant;
import org.xvm.asm.constants.TypeConstant;

import org.xvm.compiler.ErrorListener;
import org.xvm.compiler.Token;

import java.lang.reflect.Field;
import org.xvm.util.PackedInteger;


/**
 * Generic expression for something that follows the pattern "expression operator expression".
 *
 * <ul>
 * <li><tt>":"</tt> - an "else" for nullability checks</li>
 * <li><tt>"?:"</tt> - the "elvis" operator</li>
 * <li><tt>"||"</tt> - </li>
 * <li><tt>"&&"</tt> - </li>
 * <li><tt>"|"</tt> - </li>
 * <li><tt>"^"</tt> - </li>
 * <li><tt>"&"</tt> - </li>
 * <li><tt>"=="</tt> - </li>
 * <li><tt>"!="</tt> - </li>
 * <li><tt>"<"</tt> - </li>
 * <li><tt>"><tt>"</tt> - </li>
 * <li><tt>"<="</tt> - </li>
 * <li><tt>">="</tt> - </li>
 * <li><tt>"<=><tt>"</tt> - </li>
 * <li><tt>"as"</tt> - </li>
 * <li><tt>"is"</tt> - </li>
 * <li><tt>"instanceof"</tt> - </li>
 * <li><tt>".."</tt> - </li>
 * <li><tt>"<<"</tt> - </li>
 * <li><tt>">><tt>"</tt> - </li>
 * <li><tt>">>><tt>"</tt> - </li>
 * <li><tt>"+"</tt> - </li>
 * <li><tt>"-"</tt> - </li>
 * <li><tt>"*"</tt> - </li>
 * <li><tt>"/"</tt> - </li>
 * <li><tt>"%"</tt> - </li>
 * <li><tt>"/%"</tt> - </li>
 * </ul>
 */
public class BiExpression
        extends Expression
    {
    // ----- constructors --------------------------------------------------------------------------

    public BiExpression(Expression expr1, Token operator, Expression expr2)
        {
        this.expr1    = expr1;
        this.operator = operator;
        this.expr2    = expr2;
        }


    // ----- accessors -----------------------------------------------------------------------------

    @Override
    public TypeExpression toTypeExpression()
        {
        switch (operator.getId())
            {
            case ADD:
            case BIT_OR:
                return new BiTypeExpression(expr1.toTypeExpression(), operator, expr2.toTypeExpression());

            default:
                return super.toTypeExpression();
            }
        }

    @Override
    public boolean validateCondition(ErrorListener errs)
        {
        switch (operator.getId())
            {
            case BIT_AND:
            case COND_AND:
            case BIT_OR:
            case COND_OR:
                return expr1.validateCondition(errs) && expr2.validateCondition(errs);

            default:
                return super.validateCondition(errs);
            }
        }

    @Override
    public ConditionalConstant toConditionalConstant()
        {
        switch (operator.getId())
            {
            case BIT_AND:
            case COND_AND:
                return expr1.toConditionalConstant().addAnd(expr2.toConditionalConstant());

            case BIT_OR:
            case COND_OR:
                return expr1.toConditionalConstant().addOr(expr2.toConditionalConstant());

            default:
                return super.toConditionalConstant();
            }
        }

    @Override
    public long getStartPosition()
        {
        return expr1.getStartPosition();
        }

    @Override
    public long getEndPosition()
        {
        return expr2.getEndPosition();
        }

    @Override
    protected Field[] getChildFields()
        {
        return CHILD_FIELDS;
        }


    // ----- compilation ---------------------------------------------------------------------------

    @Override
    public boolean isConstant()
        {
        if (expr1.isConstant() && expr2.isConstant())
            {
            return true;
            }

        if (expr1.isConstant())
            {
            switch (operator.getId())
                {
                case COLON:
                    // if the thing on the left of the colon evaluates to a constant value, then
                    // that is the result
                    return true;

                case COND_ELSE:
                    // as long as the thing on the left of the colon is not null, then the result
                    // is the thing on the left of the colon, which is constant
                    return !expr1.isConstantNull();

                case COND_OR:
                    // short circuit logic
                    return expr1.isConstantTrue();

                case COND_AND:
                    // short circuit logic
                    return expr1.isConstantFalse();

                default:
                    // - each of these could have side-effects from the right hand side, so they are
                    //   not considered to be constant, even if we know what the resulting value is
                    // - or they are comparisons, so we'd have to know both the left AND right hand
                    //   side values
                    // - or interval/range can't be constant if we don't know the upper limit
                    return false;
                }
            }

        return false;
        }

    @Override
    public Argument generateConstant(TypeConstant constType, ErrorListener errs)
        {
        if (isConstant())
            {
            ConstantPool pool = pool();
            switch (operator.getId())
                {
                case COLON:
                    // TODO
                    throw new UnsupportedOperationException();

                case COND_ELSE:
                    return (expr1.isConstantNull() ? expr2 : expr1).generateConstant(constType, errs);

                case BIT_OR:
                    if (constType.equals(pool.typeIntLiteral()))
                        {
                        Argument arg1 = expr1.generateConstant(constType, errs);
                        Argument arg2 = expr1.generateConstant(constType, errs);
                        if (arg1 instanceof LiteralConstant && arg2 instanceof LiteralConstant)
                            {
                            PackedInteger pi1      = ((LiteralConstant) arg1).getIntegerValue();
                            PackedInteger pi2      = ((LiteralConstant) arg2).getIntegerValue();
                            int           radix    = ((LiteralConstant) arg1).getIntegerRadix();
                            PackedInteger piResult = pi1.isBig() || pi2.isBig()
                                    ? new PackedInteger(pi1.getBigInteger().or(pi2.getBigInteger()))
                                    : PackedInteger.valueOf(pi1.getLong() | pi2.getLong());
                            return pool.ensureLiteralConstant(Format.IntLiteral, piResult.toString(radix));
                            }
                        }
                    else if (constType.equals(pool.typeInt()))
                        {
                        Argument arg1 = expr1.generateConstant(constType, errs);
                        Argument arg2 = expr1.generateConstant(constType, errs);
                        if (arg1 instanceof LiteralConstant && arg2 instanceof LiteralConstant)
                            {
                            PackedInteger pi1      = ((LiteralConstant) arg1).getIntegerValue();
                            PackedInteger pi2      = ((LiteralConstant) arg2).getIntegerValue();
                            int           radix    = ((LiteralConstant) arg1).getIntegerRadix();
                            PackedInteger piResult = pi1.isBig() || pi2.isBig()
                                    ? new PackedInteger(pi1.getBigInteger().or(pi2.getBigInteger()))
                                    : PackedInteger.valueOf(pi1.getLong() | pi2.getLong());
                            return pool.ensureLiteralConstant(Format.IntLiteral, piResult.toString(radix));
                            }
                        }
                    // else if (...) TODO Int and UInt 8-128 and Var length
                    // else if (constType.equals())     TODO type | type

                    // fall through for logical boolean "or"
                case COND_OR:
                    if (constType.equals(pool.typeBoolean()))
                        {
                        // if the first expression is a boolean true, then the result is a boolean
                        // true;  otherwise if the second expression is a boolean true, then the
                        // result is a boolean true; otherwise the result is a boolean false
                        Argument arg = expr1.generateConstant(constType, errs);
                        return pool.valTrue().equals(arg)
                                ? arg
                                : expr2.generateConstant(constType, errs);
                        }
                    break;

                case BIT_AND:
                    // TODO integer

                    // fall through for logical boolean "and"
                case COND_AND:
                    if (constType.equals(pool.typeBoolean()))
                        {
                        // if the first expression is a boolean false, then the result is a boolean
                        // false;  otherwise if the second expression is a boolean true, then the
                        // result is a boolean true; otherwise the result is a boolean false
                        Argument arg = expr1.generateConstant(constType, errs);
                        return pool.valFalse().equals(arg)
                                ? arg
                                : expr2.generateConstant(constType, errs);
                        }
                    break;

                case BIT_XOR:
                case COMP_EQ:
                case COMP_NEQ:
                case COMP_LT:
                case COMP_GT:
                case COMP_LTEQ:
                case COMP_GTEQ:
                case COMP_ORD:
                case AS:
                case IS:
                case INSTANCEOF:
                case DOTDOT:
                case SHL:
                case SHR:
                case USHR:

                case ADD:
                    // applies to:
                    // Int8/16/32/64/128, VarInt
                    // UInt8/16/32/64/128, VarUInt
                    // Dec32/64/128, VarDec
                    // Float16/32/64/128, VarFloat
                    // String
                    // Type

                case SUB:
                case MUL:

                case DIVMOD:
                    // TODO same as DIV? or a Tuple result? and if so, then shouldn't all support that?
                    // fall through
                case DIV:

                case MOD:
                }
            }

        return super.generateConstant(constType, errs);
        }

//            switch (operator.getId())
//                {
//                case COLON:
//                case COND_ELSE:
//                case COND_OR:
//                case COND_AND:
//                case BIT_OR:
//                case BIT_XOR:
//                case BIT_AND:
//                case COMP_EQ:
//                case COMP_NEQ:
//                case COMP_LT:
//                case COMP_GT:
//                case COMP_LTEQ:
//                case COMP_GTEQ:
//                case COMP_ORD:
//                case AS:
//                case IS:
//                case INSTANCEOF:
//                case DOTDOT:
//                case SHL:
//                case SHR:
//                case USHR:
//                case ADD:
//                case SUB:
//                case MUL:
//                case DIV:
//                case MOD:
//                case DIVMOD:
//                }


    // ----- debugging assistance ------------------------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append(expr1)
          .append(' ')
          .append(operator.getId().TEXT)
          .append(' ')
          .append(expr2);

        return sb.toString();
        }

    @Override
    public String getDumpDesc()
        {
        return toString();
        }


    // ----- fields --------------------------------------------------------------------------------

    protected Expression expr1;
    protected Token      operator;
    protected Expression expr2;

    private static final Field[] CHILD_FIELDS = fieldsForNames(BiExpression.class, "expr1", "expr2");
    }
