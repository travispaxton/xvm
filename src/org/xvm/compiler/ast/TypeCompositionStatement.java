package org.xvm.compiler.ast;


import org.xvm.asm.ClassStructure;
import org.xvm.asm.Component;
import org.xvm.asm.Component.Format;
import org.xvm.asm.Constants.Access;
import org.xvm.asm.FileStructure;

import org.xvm.compiler.Compiler;
import org.xvm.compiler.CompilerException;
import org.xvm.compiler.ErrorListener;
import org.xvm.compiler.Source;
import org.xvm.compiler.Token;

import org.xvm.util.Severity;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.List;

import static org.xvm.compiler.Lexer.CR;
import static org.xvm.compiler.Lexer.LF;
import static org.xvm.compiler.Lexer.isLineTerminator;
import static org.xvm.compiler.Lexer.isValidQualifiedModule;
import static org.xvm.compiler.Lexer.isWhitespace;
import static org.xvm.util.Handy.appendString;
import static org.xvm.util.Handy.indentLines;


/**
 * A type declaration.
 *
 * @author cp 2017.03.28
 */
public class TypeCompositionStatement
        extends ComponentStatement
    {
    // ----- constructors --------------------------------------------------------------------------

    public TypeCompositionStatement(Source            source,
                                    long              lStartPos,
                                    long              lEndPos,
                                    Expression        condition,
                                    List<Token>       modifiers,
                                    List<Annotation>  annotations,
                                    Token             category,
                                    Token             name,
                                    List<Token>       qualified,
                                    List<Parameter>   typeParams,
                                    List<Parameter>   constructorParams,
                                    List<Composition> compositions,
                                    StatementBlock    body,
                                    Token             doc)
        {
        super(lStartPos, lEndPos);

        this.source            = source;
        this.condition         = condition;
        this.modifiers         = modifiers;
        this.annotations       = annotations;
        this.category          = category;
        this.name              = name;
        this.qualified         = qualified;
        this.typeParams        = typeParams;               
        this.constructorParams = constructorParams;        
        this.compositions      = compositions;
        this.body              = body;
        this.doc               = doc;
        }

    /**
     * Used by enumeration value declarations.
     */
    public TypeCompositionStatement(List<Annotation>     annotations,
                                    Token                name,
                                    List<TypeExpression> typeArgs,
                                    List<Expression>     args,
                                    StatementBlock       body,
                                    Token                doc,
                                    long                 lStartPos,
                                    long                 lEndPos)
        {
        super(lStartPos, lEndPos);

        this.annotations = annotations;
        this.category    = new Token(name.getStartPosition(), name.getStartPosition(), Token.Id.ENUM_VAL);
        this.name        = name;
        this.typeArgs    = typeArgs;
        this.args        = args;
        this.body        = body;
        this.doc         = doc;
        this.lStartPos   = lStartPos;
        this.lEndPos     = lEndPos;
        }

    // ----- accessors -----------------------------------------------------------------------------

    @Override
    public Access getDefaultAccess()
        {
        Access access = getAccess(modifiers);
        return access == null
                ? super.getDefaultAccess()
                : access;
        }

    public Token getCategory()
        {
        return category;
        }

    public String getName()
        {
        if (category.getId() == Token.Id.MODULE)
            {
            StringBuilder sb = new StringBuilder();
            for (Token suffix : qualified)
                {
                sb.append('.')
                  .append(suffix.getValue());
                }
            return sb.substring(1).toString();
            }
        else
            {
            return (String) name.getValue();
            }
        }

    /**
     * Determine the zone within which the type is declared. The rules for declaration change
     * depending on what the zone is; for example, the meaning of the "static" keyword differs
     * between each of the top level, inner class, and in-method zones.
     *
     * @return the declaration zone of the type represented by this TypeCompositionStatement
     */
    public Zone getDeclarationZone()
        {
        Component structThis = getComponent();
        switch (structThis.getFormat())
            {
            case MODULE:
            case PACKAGE:
                // modules and components are always top level
                return Zone.TopLevel;

            case CLASS:
            case INTERFACE:
            case SERVICE:
            case CONST:
            case ENUM:
            case TRAIT:
            case MIXIN:
                Component structParent = structThis.getParent();
                switch (structParent.getFormat())
                    {
                    case MODULE:
                    case PACKAGE:
                        return Zone.TopLevel;

                    case CLASS:
                    case INTERFACE:
                    case SERVICE:
                    case CONST:
                    case ENUM:
                    case ENUMVALUE:
                    case TRAIT:
                    case MIXIN:
                        return Zone.InClass;

                    case METHOD:
                        return Zone.InMethod;

                    default:
                        throw new IllegalStateException("this=" + structThis.getFormat()
                                + ", parent=" + structParent.getFormat());
                    }

            case ENUMVALUE:
                // enum values are ALWAYS nested inside an enumeration class
                return Zone.InClass;

            default:
                throw new IllegalStateException("this=" + structThis.getFormat());
            }
        }

    @Override
    protected Field[] getChildFields()
        {
        return CHILD_FIELDS;
        }


    // ----- compile phases ------------------------------------------------------------------------

    /**
     * Add an enclosed type composition to this type composition. Because the parser may have to
     * wrap the parsed type composition into a statement block, this method takes a Statement
     * instead of a TypeCompositionStatement, but the idea is the same: the argument to this method
     * should be an object that was returned from {@link org.xvm.compiler.Parser#parseSource()}.
     * <p/>
     * This method is used to combine multiple files that were parsed independently into a single
     * parse tree -- a single "AST" for an entire module.
     *
     * @param stmt  a statement returned from {@link org.xvm.compiler.Parser#parseSource()}
     */
    public void addEnclosed(Statement stmt)
        {
        if (enclosed == null)
            {
            if (body == null)
                {
                body = new StatementBlock(new ArrayList<>());
                }

            enclosed = new StatementBlock(new ArrayList<>());
            body.addStatement(enclosed);
            }

        enclosed.addStatement(stmt);
        }

    /**
     * Instantiate and populate the initial FileStructure for this module.
     *
     * @return  a new FileStructure for this module, with the module, packages, and classes
     *          registered
     */
    public FileStructure createModuleStructure(ErrorListener errs)
        {
        assert category.getId() == Token.Id.MODULE;     // it has to be a module!
        assert condition == null;                       // module cannot be conditional
        assert getComponent() == null;                  // it can't already have been created!

        // validate the module name
        String sName = getName();
        if (!isValidQualifiedModule(sName))
            {
            log(errs, Severity.ERROR, Compiler.MODULE_BAD_NAME, sName);
            throw new CompilerException("unable to create module with illegal name: " + sName);
            }

        registerStructures(null, errs);

        return getComponent().getFileStructure();
        }

    @Override
    protected void registerStructures(AstNode parent, ErrorListener errs)
        {
        setParent(parent);
        createStructure(errs);
        super.registerStructures(parent, errs);
        }

    /**
     * Create and populate the structure corresponding to this TypeCompositionStatement.
     *
     * @param errs  the error list to log any errors etc. to
     */
    protected void createStructure(ErrorListener errs)
        {
        assert getComponent() == null;

        // create the structure for this module, package, or class (etc.)
        String         sName     = (String) name.getValue();
        Access         access    = getDefaultAccess();
        Component      container = parent == null ? null : parent.getComponent();
        ClassStructure component = null;
        switch (category.getId())
            {
            case MODULE:
                if (container == null)
                    {
                    // create the FileStructure and "this" ModuleStructure
                    FileStructure struct = new FileStructure(getName());
                    component = struct.getModule();
                    }
                else
                    {
                    log(errs, Severity.ERROR, Compiler.MODULE_UNEXPECTED);
                    }
                break;

            case PACKAGE:
                if (container.isPackageContainer())
                    {
                    // the check for duplicates is deferred, since it is possible (thanks to
                    // the complexity of conditionals) to have multiple components occupying
                    // the same location within the namespace at this point in the compilation
                    component = container.createPackage(access, sName);
                    }
                else
                    {
                    log(errs, Severity.ERROR, Compiler.PACKAGE_UNEXPECTED, container.toString());
                    }
                break;

            case CLASS:
            case INTERFACE:
            case SERVICE:
            case CONST:
            case ENUM:
            case ENUM_VAL:
            case TRAIT:
            case MIXIN:
                if (container.isClassContainer())
                    {
                    Format format;
                    switch (category.getId())
                        {
                        case CLASS:
                            format = Format.CLASS;
                            break;

                        case INTERFACE:
                            format = Format.INTERFACE;
                            break;

                        case SERVICE:
                            format = Format.SERVICE;
                            break;

                        case CONST:
                            format = Format.CONST;
                            break;

                        case ENUM:
                            format = Format.ENUM;
                            break;

                        case ENUM_VAL:
                            format = Format.ENUMVALUE;
                            break;

                        case TRAIT:
                            format = Format.TRAIT;
                            break;

                        case MIXIN:
                            format = Format.MIXIN;
                            break;

                        default:
                            throw new IllegalStateException();
                        }

                    component = container.createClass(getDefaultAccess(), format, sName);
                    }
                else
                    {
                    log(errs, Severity.ERROR, Compiler.CLASS_UNEXPECTED, container.toString());
                    }
                break;

            default:
                throw new UnsupportedOperationException("unable to guess structure for: "
                        + category.getId().TEXT);
            }

        if (component == null)
            {
            return;
            }
        else
            {
            setComponent(component);
            }

        // the "global" namespace is composed of the union of the top-level namespace and the "inner"
        // namespace of each component in the global namespace.
        //
        // modifiers for "top-level" namespace structures:
        // - "top-level" means nested within a file, module, or package structure
        // - static means "singleton"
        // - public means visible outside of the module
        // - protected means t.b.d.
        // - private means no visibility outside of the module
        //
        //              public      protected   private     static
        //              ----------  ----------  ----------  ----------
        // module       (implicit)                          (implicit)
        // package      x           x           x           (implicit)
        // class        x           x           x
        // interface    x           x           x
        // service      x           x           x           x
        // const        x           x           x           x
        // enum         x           x           x           (implicit)
        // trait        x           x           x
        // mixin        x           x           x
        //
        // modifiers for "inner" namespace structures:
        // - "inner" means nested within a class
        // - static means "no ref to parent, no virtual new"; it only applies to something that can be new'd
        //   or init'd from a constant, so it does not apply to interface, trait, or mixin
        //
        //              public      protected   private     static
        //              ----------  ----------  ----------  ----------
        // class        x           x           x           x
        // interface    x           x           x
        // service      x           x           x           x - required if parent is not const or service
        // const        x           x           x           x - required if parent is not const
        // enum         x           x           x           (implicit)
        // - enum val                                       (implicit)
        // trait        x           x           x
        // mixin        x           x           x
        //
        // modifiers for "local" namespace structures:
        // - "local" means declared within a method; items declared within a method are not visible outside
        //   of (above on the hierarchy) the method
        // - static means "no ref to the method frame", i.e. no ability to capture, not even the "this"
        //   from the method
        //
        //              public      protected   private     static
        //              ----------  ----------  ----------  ----------
        // class                                            x
        // interface
        // service                                          x
        // const                                            x
        // enum                                             (implicit)
        // trait
        // mixin
        int nAllowed = 0;
        switch (component.getFormat())
            {
            case SERVICE:
            case CONST:
            case CLASS:
                // class is not allowed to be declared static if it is top-level, otherwise all of
                // these can always be declared static
                if (!(component.getFormat() == Format.CLASS && getDeclarationZone() == Zone.TopLevel))
                    {
                    nAllowed |= Component.STATIC_BIT;
                    }
                // fall through
            case PACKAGE:
            case ENUM:
            case INTERFACE:
            case TRAIT:
            case MIXIN:
                {
                // these are all allowed to be declared public/private/protected, except when they
                // appear inside a method body
                if (getDeclarationZone() != Zone.InMethod)
                    {
                    nAllowed |= Component.ACCESS_MASK;
                    }
                }
            }

        // validate modifiers
        boolean fExplicitlyStatic = false;
        if (modifiers != null && !modifiers.isEmpty())
            {
            int     nSpecified           = 0;
            boolean fExplicitlyPublic    = false;
            boolean fExplicitlyProtected = false;
            boolean fExplicitlyPrivate   = false;

            NextModifier: for (int i = 0, c = modifiers.size(); i < c; ++i)
                {
                Token token = modifiers.get(i);
                int     nBits;
                boolean fAlready;
                switch (token.getId())
                    {
                    case PUBLIC:
                        fAlready          = fExplicitlyPublic;
                        fExplicitlyPublic = true;
                        nBits             = Component.ACCESS_MASK;
                        break;

                    case PROTECTED:
                        fAlready             = fExplicitlyProtected;
                        fExplicitlyProtected = true;
                        nBits                = Component.ACCESS_MASK;
                        break;

                    case PRIVATE:
                        fAlready           = fExplicitlyPrivate;
                        fExplicitlyPrivate = true;
                        nBits              = Component.ACCESS_MASK;
                        break;

                    case STATIC:
                        fAlready          = fExplicitlyStatic;
                        fExplicitlyStatic = true;
                        nBits             = Component.STATIC_BIT;
                        break;

                    default:
                        throw new IllegalStateException("token=" + token);
                    }

                if (fAlready)
                    {
                    log(errs, Severity.ERROR, Compiler.DUPLICATE_MODIFIER, token.getId().TEXT);
                    }
                else if ((nAllowed & nBits) == 0)
                    {
                    log(errs, Severity.ERROR, Compiler.ILLEGAL_MODIFIER, token.getId().TEXT);
                    }
                else if ((nSpecified & nBits) != 0)
                    {
                    log(errs, Severity.ERROR, Compiler.CONFLICTING_MODIFIER, token.getId().TEXT);
                    }

                nSpecified |= nBits;
                }

            // verification that if one access modifier is explicit, that the component correctly
            // used that access modifier
            if (fExplicitlyPublic ^ fExplicitlyProtected ^ fExplicitlyPrivate)
                {
                assert (component.getAccess() == Access.PUBLIC   ) == fExplicitlyPublic;
                assert (component.getAccess() == Access.PROTECTED) == fExplicitlyProtected;
                assert (component.getAccess() == Access.PRIVATE  ) == fExplicitlyPrivate;
                }
            }

        // inner const/service classes must be declared static if the parent is not const/service
        if (!fExplicitlyStatic && getDeclarationZone() == Zone.InClass)
            {
            if (component.getFormat() == Format.CONST)
                {
                // parent MUST be a const (because parent will be automatically captured, and a
                // const can't capture a non-const)
                if (container.getFormat() != Format.CONST)
                    {
                    log(errs, Severity.ERROR, Compiler.INNER_CONST_NOT_STATIC);
                    fExplicitlyStatic = true;
                    }
                }
            else if (component.getFormat() == Format.SERVICE)
                {
                // parent MUST be a const or a service (because parent is automatically captured,
                // and a service can't capture an object that isn't either a const or a service)
                if (container.getFormat() != Format.CONST && container.getFormat() != Format.SERVICE)
                    {
                    log(errs, Severity.ERROR, Compiler.INNER_SERVIC_NOT_STATIC);
                    fExplicitlyStatic = true;
                    }
                }
            }

        // configure the static bit on the component
        if (fExplicitlyStatic || component.getFormat().isImplicitlyStatic())
            {
            component.setStatic(true);
            }

        // validate that type parameters are allowed (the actual validation of the type parameters
        // themselves happens in a later phase)
        switch (component.getFormat())
            {
            case MODULE:
            case PACKAGE:
                // type parameters are not permitted
                disallowTypeParams(errs);
                // constructor params are only allowed if they have defaulted values
                requireConstructorParamValues(errs);
                break;

            case ENUMVALUE:
                // type parameters are not permitted
                disallowTypeParams(errs);
                // number of type arguments must match the number of the enum's type parameters
                assert container instanceof ClassStructure && container.getFormat() == Format.ENUM;
// TODO make sure # matches               if ((args == null ? 0 : args.size()) != typeParams)
// List<Expression>     args;
                    break;

            case SERVICE:
            case CONST:
            case CLASS:
                // these compositions are new-able, and thus can usually declare type parameters;
                // the exception is when the composition is not new-able, which is the case for
                // singleton compositions
                if (fExplicitlyStatic && getDeclarationZone() == Zone.TopLevel)
                    {
                    disallowTypeParams(errs);
                    requireConstructorParamValues(errs);
                    }
                break;

            case ENUM:
                // while an enum is not new-able (it is abstract), it can have type params which are
                // then defined by each enum value; the same goes for constructor params
                break;

            case INTERFACE:
            case TRAIT:
            case MIXIN:
                break;

            default:
                throw new IllegalStateException();
            }

        // validate constructor parameters
        // TODO
        // constructor parameters are not permitted unless they all have default values (since the
        // module is a singleton, and is automatically created, i.e. it has to have all of its
        // construction parameters available)


        // validate composition
        boolean fAlreadyExtends = false;
        for (Composition composition : compositions)
            {
            switch (composition.getKeyword().getId())
                {
                case EXTENDS:
                    // only one extends is allowed
                    if (fAlreadyExtends)
                        {
                        Token token = composition.getKeyword();
                        log(errs, Severity.ERROR, Compiler.MULTIPLE_EXTENDS, composition);
                        }
                    else
                        {
                        // make sure that there is only one "extends" clause, but defer the analysis
                        // of conditional "extends" clauses (since we can't evaluate conditions yet)
                        fAlreadyExtends = composition.condition == null;
                        }
                    break;

                case DELEGATES:
                case IMPLEMENTS:
                case INCORPORATES:
                    // these are all OK; other checks will be done after the types are resolved
                    break;

                case IMPORT:
                case IMPORT_EMBED:
                case IMPORT_REQ:
                case IMPORT_WANT:
                case IMPORT_OPT:
                case INTO:
                    // "import" composition not allowed for modules (only used by packages)
                    // "into" not allowed (only used by traits & mixins)
                    Token token = composition.getKeyword();
                    log(errs, Severity.ERROR, Compiler.KEYWORD_UNEXPECTED, composition);
                    break;
                }
            }

        // validate and register annotations (as if they had been written as "incorporates" clauses)
        if (annotations != null && !annotations.isEmpty())
            {
            if (compositions == null)
                {
                compositions = new ArrayList<>();
                }
            for (int i = annotations.size()-1; i >= 0; --i)
                {
                compositions.add(new Composition.Incorporates(annotations.get(i)));
                }
            }

        if (doc != null)
            {
            component.setDocumentation(extractDocumentation(doc));
            }

        // TODO validate any constructor parameters and their default values, and transfer the info to the constructor

        }

    private void disallowTypeParams(ErrorListener errs)
        {
        // type parameters are not permitted
        if (typeParams != null && !typeParams.isEmpty())
            {
            // note: currently no way to determine the location of the parameters
            // Parameter paramFirst = typeParams.get(0);
            // Parameter paramLast  = typeParams.get(typeParams.size() - 1);

            Token tokFirst = category == null ? name : category;
            Token tokLast = name == null ? category : name;
            log(errs, Severity.ERROR, Compiler.TYPE_PARAMS_UNEXPECTED);
            }
        }

    private void disallowConstructorParams(ErrorListener errs)
        {
        // constructor parameters are not permitted
        if (constructorParams != null && !constructorParams.isEmpty())
            {
            // note: currently no way to determine the location of the parameters
            // Parameter paramFirst = constructorParams.get(0);
            // Parameter paramLast  = constructorParams.get(constructorParams.size() - 1);

            Token tokFirst = category == null ? name : category;
            Token tokLast  = name == null ? category : name;
            log(errs, Severity.ERROR, Compiler.CONSTRUCTOR_PARAMS_UNEXPECTED);
            }
        }

    private void requireConstructorParamValues(ErrorListener errs)
        {
        // constructor parameters are not permitted
        if (constructorParams != null && !constructorParams.isEmpty())
            {
            for (Parameter param : constructorParams)
                {
                if (param.value == null)
                    {
                    // note: currently no way to determine the location of the parameter
                    Token tokFirst = category == null ? name : category;
                    Token tokLast  = name == null ? category : name;
                    log(errs, Severity.ERROR, Compiler.CONSTRUCTOR_PARAM_DEFAULT_REQUIRED);
                    }
                }
            }
        }

    /**
     * Parse a documentation comment, extracting the "body" of the documentation inside it.
     *
     * @param token  a documentation token
     *
     * @return the "body" of the documentation, as LF-delimited lines, without the leading "* "
     */
    public static String extractDocumentation(Token token)
        {
        if (token == null)
            {
            return null;
            }

        String sDoc = (String) token.getValue();
        if (sDoc == null || sDoc.length() <= 1 || sDoc.charAt(0) != '*')
            {
            return null;
            }

        StringBuilder sb = new StringBuilder();
        int nState = 0;
        NextChar: for (char ch : sDoc.substring(1).toCharArray())
            {
            switch (nState)
                {
                case 0:         // leading whitespace expected
                    if (!isLineTerminator(ch))
                        {
                        if (isWhitespace(ch))
                            {
                            continue NextChar;
                            }

                        if (ch == '*')
                            {
                            nState = 1;
                            continue NextChar;
                            }

                        // weird - it's actual text to append; we didn't find the leading '*'
                        break;
                        }
                    // fall through

                case 1:         // ate the asterisk; expecting one space
                    if (!isLineTerminator(ch))
                        {
                        if (isWhitespace(ch))
                            {
                            nState = 2;
                            continue NextChar;
                            }

                        // weird - it's actual text to append; there was no ' ' after the '*'
                        break;
                        }
                    // fall through

                case 2:         // in the text
                    if (isLineTerminator(ch))
                        {
                        if (sb.length() > 0)
                            {
                            sb.append(LF);
                            }
                        nState = ch == CR ? 3 : 0;
                        continue NextChar;
                        }
                    break;

                case 3:         // ate a CR, emitted an LF
                    if (ch == LF || isWhitespace(ch))
                        {
                        nState = 0;
                        continue NextChar;
                        }

                    if (ch == '*')
                        {
                        nState = 1;
                        continue NextChar;
                        }

                    // weird - it's actual text to append; we didn't find the leading '*'
                    break;
                }

            nState = 2;
            sb.append(ch);
            }

        // trim any trailing whitespace & line terminators
        int cch = sb.length();
        while (isWhitespace(sb.charAt(--cch)))
            {
            sb.setLength(cch);
            }

        return sb.toString();
        }


    // ----- debugging assistance ------------------------------------------------------------------

    public String toSignatureString()
        {
        StringBuilder sb = new StringBuilder();

        if (category.getId() == Token.Id.ENUM_VAL)
            {
            if (annotations != null)
                {
                for (Annotation annotation : annotations)
                    {
                    sb.append(annotation)
                      .append(' ');
                    }
                }

            sb.append(name.getValue());

            if (typeParams != null)
                {
                sb.append('<');
                boolean first = true;
                for (TypeExpression typeParam : typeArgs)
                    {
                    if (first)
                        {
                        first = false;
                        }
                    else
                        {
                        sb.append(", ");
                        }
                    sb.append(typeParam);
                    }
                sb.append('>');
                }

            if (args != null)
                {
                sb.append('(');
                boolean first = true;
                for (Expression arg : args)
                    {
                    if (first)
                        {
                        first = false;
                        }
                    else
                        {
                        sb.append(", ");
                        }
                    sb.append(arg);
                    }
                sb.append(')');
                }
            }
        else
            {
            if (modifiers != null)
                {
                for (Token token : modifiers)
                    {
                    sb.append(token.getId().TEXT)
                      .append(' ');
                    }
                }

            if (annotations != null)
                {
                for (Annotation annotation : annotations)
                    {
                    sb.append(annotation)
                      .append(' ');
                    }
                }

            sb.append(category.getId().TEXT)
              .append(' ');

            if (qualified == null)
                {
                sb.append(name.getValue());
                }
            else
                {
                boolean first = true;
                for (Token token : qualified)
                    {
                    if (first)
                        {
                        first = false;
                        }
                    else
                        {
                        sb.append('.');
                        }
                    sb.append(token.getValue());
                    }
                }

            if (typeParams != null)
                {
                sb.append('<');
                boolean first = true;
                for (Parameter param : typeParams)
                    {
                    if (first)
                        {
                        first = false;
                        }
                    else
                        {
                        sb.append(", ");
                        }
                    sb.append(param.toTypeParamString());
                    }
                sb.append('>');
                }

            if (constructorParams != null)
                {
                sb.append('(');
                boolean first = true;
                for (Parameter param : constructorParams)
                    {
                    if (first)
                        {
                        first = false;
                        }
                    else
                        {
                        sb.append(", ");
                        }
                    sb.append(param);
                    }
                sb.append(')');
                }
            }

        return sb.toString();
        }

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        if (doc != null)
            {
            String sDoc = String.valueOf(doc.getValue());
            if (sDoc.length() > 100)
                {
                sDoc = sDoc.substring(0, 97) + "...";
                }
            appendString(sb.append("/*"), sDoc).append("*/\n");
            }

        sb.append(toSignatureString());

        if (category.getId() == Token.Id.ENUM_VAL)
            {
            if (body != null)
                {
                sb.append('\n')
                  .append(indentLines(body.toString(), "    "));
                }
            }
        else
            {
            for (Composition composition : this.compositions)
                {
                sb.append("\n        ")
                  .append(composition);
                }

            if (body == null)
                {
                sb.append(';');
                }
            else
                {
                String sBody = body.toString();
                if (sBody.indexOf('\n') >= 0)
                    {
                    sb.append('\n')
                      .append(indentLines(sBody, "    "));
                    }
                else
                    {
                    sb.append(' ')
                      .append(sBody);
                    }
                }
            }

        return sb.toString();
        }

    @Override
    public String getDumpDesc()
        {
        return toSignatureString();
        }


    // ----- inner class: Zone ---------------------------------------------------------------------

    /**
     * The Zone enumeration defines the zone within which a particular type is declared.
     *
     * <ul>
     * <li><b>{@code TopLevel}</b> - the module itself, or declared within a module or package;</li>
     * <li><b>{@code InClass}</b> - declared within a class, e.g. an inner class;</li>
     * <li><b>{@code InMethod}</b> - declared within the body of a method.</li>
     * </ul>
     */
    public enum Zone
        {
        TopLevel, InClass, InMethod;

        /**
         * Look up a DeclZone enum by its ordinal.
         *
         * @param i  the ordinal
         *
         * @return the DeclZone enum for the specified ordinal
         */
        public static Zone valueOf(int i)
            {
            return ZONES[i];
            }

        /**
         * All of the DeclZone enums.
         */
        private static final Zone[] ZONES = Zone.values();
        }


    // ----- fields --------------------------------------------------------------------------------

    protected Source               source;
    protected Expression           condition;
    protected List<Token>          modifiers;
    protected List<Annotation>     annotations;
    protected Token                category;
    protected Token                name;
    protected List<Token>          qualified;
    protected List<Parameter>      typeParams;
    protected List<Parameter>      constructorParams;
    protected List<TypeExpression> typeArgs;
    protected List<Expression>     args;
    protected List<Composition>    compositions;
    protected StatementBlock       body;
    protected Token                doc;
    protected StatementBlock       enclosed;

    private static final Field[] CHILD_FIELDS = fieldsForNames(TypeCompositionStatement.class,
            "annotations", "typeParams", "constructorParams", "typeArgs", "args", "compositions",
            "body");
    }
