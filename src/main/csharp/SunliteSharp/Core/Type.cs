using System.Runtime.InteropServices.ComTypes;
using System.Text;
using SunliteSharp.Core.Enum;
using SunliteSharp.Runtime;

namespace SunliteSharp.Core;

public abstract record Type
{
    public abstract string Name();

    public record Singular(PrimitiveType Type, string Ref = "") : Type
    {
        public override string Name()
        {
            return Type.ToString();
        }

        public override string ToString()
        {
            return Name();
        }
    }

    public record Reference(PrimitiveType Type, string Ref, Type ReturnType, List<Param> Params, List<Param> TypeParams) : Singular(Type, Ref)
    {
        public override string Name()
        {
            return Ref;
        }

        public override string ToString()
        {
            var s = new StringBuilder($"{Type.ToString()}");
            return Type switch
            {
                PrimitiveType.Function => s.Append($" '{Ref}({string.Join(", ",Params.Select(p => p.Type.Name()))}): {ReturnType}'").ToString(),
                PrimitiveType.Table => s.Append($" '{TypeParams[0].Type}' -> {ReturnType}").ToString(),
                PrimitiveType.Array => s.Append($" '{ReturnType}'").ToString(),
                _ => s.Append($" {(Ref != "" ? $"'{Ref}'" : "")}").ToString()
            };
        }

        public virtual bool Equals(Reference? other)
        {
            if (other is null) return false;
            if(other.Type != Type) return false;
            switch (Type)
            {
                case PrimitiveType.Function:
                    if (ReturnType == other.ReturnType)
                    {
                        if (Params.Count != other.Params.Count) return false;

                        IEnumerable<Type> types = Params.Select(p => p.Type);
                        IEnumerable<Type> otherTypes = other.Params.Select(p => p.Type);

                        return types.Zip(otherTypes, (t1, t2) => new { t1, t2 }).All(pair => Contains(pair.t2, pair.t1, sl));
                    }
                    break;

                case PrimitiveType.Object:
                    if (sl == null) return false;
                    return Ref == other.Ref || TraverseTypeHierarchy(other.Ref);

                case PrimitiveType.Array:
                    return Contains(ReturnType, other.ReturnType, sl);

                case PrimitiveType.Table:
                    return Contains(TypeParams[0].Type, other.TypeParams[0].Type, sl) && Contains(ReturnType, other.ReturnType, sl);

                case PrimitiveType.Class:
                    if (string.IsNullOrEmpty(Ref)) return true;
                    if (Ref == other.Ref) return true;
                    break;

                default:
                    return true;
            }

            return false;
        }
        
        private bool TraverseTypeHierarchy(string other)
        {
            (string, List<string>, List<string>)? parents = sl?.Collector?.TypeHierarchy.GetValueOrDefault(other);
            var superclass = parents?.Item1;
            List<string>? interfaces = parents?.Item2;

            if (superclass == Ref) return true;
            if (interfaces != null && interfaces.Contains(Ref)) return true;

            if ((string.IsNullOrEmpty(superclass) || superclass == "<nil>") &&
                (interfaces == null || interfaces.Count == 0))
            {
                return false;
            }

            if (!string.IsNullOrEmpty(superclass))
            {
                var success = TraverseTypeHierarchy(superclass);
                if (success) return true;
            }

            if (interfaces != null)
            {
                return interfaces.Select(TraverseTypeHierarchy).Any(success => success);
            }

            return false;
        }


        public override int GetHashCode()
        {
            return HashCode.Combine(base.GetHashCode(), ReturnType, Params, TypeParams);
        }
    }

    public record Union(List<Singular> Types) : Type
    {
        public override string Name()
        {
            return string.Join(" | ", Types.Select(t => t.ToString()));
        }

        public override string ToString()
        {
            return Name();
        }
    }

    public record Parameter(Token ParamName) : Singular(PrimitiveType.Generic, ParamName.Lexeme)
    {
        public override string Name()
        {
            return ParamName.Lexeme;
        }

        public override string ToString()
        {
            return $"type parameter '{ParamName.Lexeme}'";
        }
    }
    
    public static bool Contains(Type type, Type inType, Sunlite? sunlite = null)
    {
        sl = sunlite;
        if (type == Unknown) return false;
        if (inType is Union inU)
        {
            if (type is Union u)
            {
                return inU.Types.SequenceEqual(u.Types) || inU.Types.Contains(Any);
            }

            return inU.Types.Contains(type) || inU.Types.Contains(Any);
        }
        else
        {
            if (type is Union u)
            {
                return u.Types.Contains(type) || inType == Any;
            }

            return inType.Equals(type) || inType == Any;
        }
    }

    public static Type FromValue(object? value, Sunlite sunlite)
    {
        return value switch
        {
            Type type => type,
            Param param => param.Type,
            string => String,
            double => Number,
            bool => Boolean,
            SLNil => Nil,
            SLClosure closure => OfFunction(closure.Function.Name, closure.Function.ReturnType, closure.Function.Params),
            SLBoundMethod boundMethod => OfFunction(boundMethod.Method.Function.Name, boundMethod.Method.Function.ReturnType, boundMethod.Method.Function.Params),
            SLUpvalue upvalue => FromValue(upvalue.ClosedValue, sunlite),
            SLNativeFunction nativeFunc => OfFunction(nativeFunc.Name, nativeFunc.ReturnType, []),
            SLFunction function => OfFunction(function.Name, function.ReturnType, function.Params),
            SLClass clazz => OfClass(clazz.Name, []),
            SLClassInstance instance => OfObject(instance.Clazz.Name),
            SLArray => Array,
            SLTable => Table,
            SLType slType => slType.Value,
            _ => Unknown
        };
    }

    public static Type Of(List<TypeToken> tokens, Sunlite sunlite, bool topmost = true)
    {
        switch (tokens.Count)
        {
            case 0:
                throw new ArgumentException("No types provided.");
            case > 1 when topmost:
                throw new ArgumentException("There can be only one topmost type.");
        }

        if (topmost)
        {
            (Dictionary<Token, List<TypeToken>> typeTokens, List<TypeToken> typeParameters) = tokens.First();
            if (typeTokens.Count == 1)
            {
                KeyValuePair<Token, List<TypeToken>> singleToken = typeTokens.First();
                switch (singleToken.Key.Type)
                {
                    case TokenType.TypeFunction:
                        var returnType = typeParameters.Count == 0 ? Nil : Of([typeParameters.Last()], sunlite, false);
                        List<TypeToken> paramTokens = typeParameters.Take(typeParameters.Count - 1).ToList();
                        List<Param> parameters = paramTokens.Select(t => new Param(Token.Identifier("",""), Of([t], sunlite, false))).ToList();
                        return OfFunction("", returnType, parameters);
                    case TokenType.TypeArray:
                        var elementType = typeParameters.Count == 0
                            ? NullableAny
                            : Of(typeParameters, sunlite, false);
                        return OfArray(elementType);
                    case TokenType.TypeTable:
                        var keyType = typeParameters.Count == 0
                            ? NullableAny
                            : Of([typeParameters[0]], sunlite, false);
                        var valueType = typeParameters.Count == 0
                            ? NullableAny
                            : Of([typeParameters[1]], sunlite, false);
                        return OfTable(keyType, valueType);
                    case TokenType.TypeClass:
                        if(typeParameters.Count == 0)
                        {
                            return OfClass("",[]);
                        }
                        var className =
                            typeParameters.First().Tokens.First().Key.Lexeme;
                        return OfClass(className,[]);
                    case TokenType.Identifier:
                        if (typeParameters.Count > 0)
                        {
                            List<Type> typeParamTypes = typeParameters.Select(tp => Of([tp], sunlite, false)).ToList();
                            var name = singleToken.Key.Lexeme;
                            List<string>? typeParams = sunlite.Collector?.TypeHierarchy.TryGetValue(name, out var tuple) == true ? tuple.Item3 : null;
                            return OfGenericObject(
                                singleToken.Key.Lexeme,
                                typeParamTypes.Select(
                                    (t, i) => 
                                        new Param(
                                            Token.Identifier(
                                                typeParams != null && i < typeParams.Count ? typeParams[i] : "?", 
                                                "<unknown>"), 
                                            t)).ToList());
                        }
                        return OfObject(singleToken.Key.Lexeme);
                    case TokenType.TypeGeneric:
                        return new Parameter(typeParameters.First().Tokens.First().Key);
                    default:
                        return new Singular(PrimitiveTypeExtensions.Get(singleToken.Key));
                }
            }

            List<Singular> types = typeTokens.Values.Select(t => (Of(t, sunlite, false) as Singular)!).ToList();
            return new Union(types);
        }

        if (tokens.Count == 1)
        {
            (Dictionary<Token, List<TypeToken>> typeTokens, List<TypeToken> typeParameters) = tokens.First();
            if (typeTokens.Count == 1)
            {
                KeyValuePair<Token, List<TypeToken>> singleToken = typeTokens.First();
                switch (singleToken.Key.Type)
                {
                    case TokenType.TypeFunction:
                        var returnType = typeParameters.Count == 0 ? Nil : Of([typeParameters.Last()], sunlite, false);
                        List<TypeToken> paramTokens = typeParameters.Take(typeParameters.Count - 1).ToList();
                        List<Param> parameters = paramTokens.Select(t => new Param(Token.Identifier("",""), Of([t], sunlite, false))).ToList();
                        return OfFunction("", returnType, parameters);
                    case TokenType.TypeArray:
                        var elementType = typeParameters.Count == 0
                            ? NullableAny
                            : Of(typeParameters, sunlite, false);
                        return OfArray(elementType);
                    case TokenType.TypeTable:
                        var keyType = typeParameters.Count == 0
                            ? NullableAny
                            : Of([typeParameters[0]], sunlite, false);
                        var valueType = typeParameters.Count == 0
                            ? NullableAny
                            : Of([typeParameters[1]], sunlite, false);
                        return OfTable(keyType, valueType);
                    case TokenType.TypeClass:
                        if (typeParameters.Count == 0)
                        {
                            return OfClass("", []);
                        }

                        var className =
                            typeParameters.First().Tokens.First().Key.Lexeme;
                        return OfClass(className, []);
                    case TokenType.Identifier:
                        if (typeParameters.Count > 0)
                        {
                            List<Type> typeParamTypes = typeParameters.Select(tp => Of([tp], sunlite, false)).ToList();
                            var name = singleToken.Key.Lexeme;
                            List<string>? typeParams = sunlite.Collector?.TypeHierarchy.TryGetValue(name, out var tuple) == true ? tuple.Item3 : null;
                            return OfGenericObject(
                                singleToken.Key.Lexeme,
                                typeParamTypes.Select(
                                    (t, i) => 
                                        new Param(
                                            Token.Identifier(
                                                typeParams != null && i < typeParams.Count ? typeParams[i] : "?", 
                                                "<unknown>"), 
                                            t)).ToList());
                        }
                        return OfObject(singleToken.Key.Lexeme);
                    case TokenType.TypeGeneric:
                        return new Parameter(typeParameters.First().Tokens.First().Key);
                    default:
                        return new Singular(PrimitiveTypeExtensions.Get(singleToken.Key));
                }
            }

            List<Singular> types = typeTokens.Select(t => (Of(t.Value, sunlite, false) as Singular)!).ToList();
            return new Union(types);
        }
        else
        {
            List<Singular> types = tokens.Select(t => (Of([t], sunlite, false) as Singular)!).ToList();
            return new Union(types);
        }
    }
    
    public static Reference OfClass(string name, List<Param> parameters)
    {
        return new Reference(PrimitiveType.Class, name, OfObject(name), parameters, []);
    }

    public static Reference OfObject(string name)
    {
        var reference = new Reference(PrimitiveType.Object, name, Object, [], []);
        return new Reference(PrimitiveType.Object, name, reference, [], []);
    }
    
    public static Reference OfGenericObject(string name, List<Param> typeParams)
    {
        var reference = new Reference(PrimitiveType.Object, name, Object, [], typeParams);
        return new Reference(PrimitiveType.Object, name, reference, [], typeParams);
    }

    public static Reference OfFunction(string name, Type returnType, List<Param> parameters)
    {
        return new Reference(PrimitiveType.Function, name, returnType, parameters, []);
    }

    public static Reference OfGenericFunction(string name, Type returnType, List<Param> parameters, List<Param> typeParams)
    {
        return new Reference(PrimitiveType.Function, name, returnType, parameters, typeParams);
    }

    public static Reference OfArray(Type elementType)
    {
        return new Reference(PrimitiveType.Array, "<array>", elementType, [], []);
    }

    public static Reference OfTable(Type keyType, Type valueType)
    {
        return new Reference(PrimitiveType.Table, "<table>", valueType, [], [new Param(Token.Identifier("<key>",""), keyType)]);
    }

    public static readonly Singular Unknown = new(PrimitiveType.Unknown);
    public static readonly Singular Nil = new(PrimitiveType.Nil);
    public static readonly Singular Any = new(PrimitiveType.Any);
    public static readonly Union NullableAny = new([Any, Nil]);
    public static readonly Singular Number = new(PrimitiveType.Number);
    public static readonly Singular String = new(PrimitiveType.String);
    public static readonly Singular Boolean = new(PrimitiveType.Boolean);
    public static readonly Singular Function = new(PrimitiveType.Function);
    public static readonly Singular Class = new(PrimitiveType.Class);
    public static readonly Singular Object = new(PrimitiveType.Object);
    public static readonly Singular Array = new(PrimitiveType.Array);
    public static readonly Singular Table = new(PrimitiveType.Table);

    public static Sunlite? sl = null;
}