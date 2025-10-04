// ReSharper disable InconsistentNaming

using Type = SunliteSharp.Core.Type;

namespace SunliteSharp.Runtime;

public abstract record AnySLValue
{
    public abstract AnySLValue Copy();
    public abstract object Get();
    public abstract bool IsObj();
}

public abstract record SLValue<T> (T Value): AnySLValue where T : notnull
{
    public abstract override SLValue<T> Copy();

    public override bool IsObj()
    {
        return false;
    }

    public override object Get()
    {
        return Value;
    }
    
    public override string ToString()
    {
        return Value.ToString() ?? "null";
    }

    public virtual bool Equals(SLValue<T>? other)
    {
        if (other is null) return false;
        return ReferenceEquals(this, other) || EqualityComparer<T>.Default.Equals(Value, other.Value);
    }

    public override int GetHashCode()
    {
        return EqualityComparer<T>.Default.GetHashCode(Value);
    }
}

public record SLBool(bool Value) : SLValue<bool>(Value)
{
    public static readonly SLBool True = new(true);
    public static readonly SLBool False = new(false);
    
    public override SLValue<bool> Copy()
    {
        return new SLBool(Value);
    }
    
    public override string ToString()
    {
        return Value.ToString();
    }
}

public record SLNumber(double Value) : SLValue<double>(Value), IComparable<SLNumber>
{
    public override SLValue<double> Copy()
    {
        return new SLNumber(Value);
    }
    
    public static SLNumber operator -(SLNumber n) => new(-n.Value);
    
    public static SLNumber operator +(SLNumber a, SLNumber b) => new(a.Value + b.Value);
    public static SLNumber operator -(SLNumber a, SLNumber b) => new(a.Value - b.Value);
    public static SLNumber operator *(SLNumber a, SLNumber b) => new(a.Value * b.Value);
    public static SLNumber operator /(SLNumber a, SLNumber b) => new(a.Value / b.Value);
    
    public static SLBool operator >(SLNumber a, SLNumber b) => new(a.Value > b.Value);
    public static SLBool operator <(SLNumber a, SLNumber b) => new(a.Value > b.Value);
    
    public static SLNumber operator ++(SLNumber n) => new(n.Value + 1);
    public static SLNumber operator --(SLNumber n) => new(n.Value - 1);
    

    public int CompareTo(SLNumber? other)
    {
        return other?.Value.CompareTo(Value) ?? 0;
    }
    
    public override string ToString()
    {
        return Value.ToString();
    }
}

public readonly struct Nil
{
    public static readonly Nil Value = new();
}

public sealed record SLNil() : SLValue<Nil>(Runtime.Nil.Value)
{
    public static readonly SLNil Nil = new();

    public override SLValue<Nil> Copy()
    {
       return Nil;
    }

    public override string ToString()
    {
        return "<nil>";
    }

    public bool Equals(SLNil? other)
    {
        return other is not null;
    }

    public override int GetHashCode()
    {
        return 0;
    }
}

public abstract record SLObj<T>(T Value) : SLValue<T>(Value) where T : notnull
{
    public override bool IsObj()
    {
        return true;
    }
    
    public override string ToString()
    {
        return Value.ToString() ?? "null";
    }
};

public record SLString(string Value) : SLObj<string>(Value)
{
    public override SLValue<string> Copy()
    {
        return new SLString(Value);
    }
    
    public static SLString operator +(SLString a, SLString b) => new(a.Value + b.Value);
    
    public override string ToString()
    {
        return $"\"{Value}\"";
    }
}

public record SLType(Type Value) : SLObj<Type>(Value)
{
    public virtual bool Equals(SLType? other)
    {
        return other is not null && Type.Contains(other.Value, Value);
    }

    public override int GetHashCode()
    {
        return base.GetHashCode();
    }

    public override SLValue<Type> Copy()
    {
        return new SLType(Value);
    }

    public override string ToString()
    {
        return Value.ToString();
    }
}

public record SLFuncObj(SLFunction Value) : SLObj<SLFunction>(Value)
{
    public override SLValue<SLFunction> Copy()
    {
        return new SLFuncObj(Value.Copy());
    }
    
    public override string ToString()
    {
        return Value.ToString();
    }
}

public record SLClosureObj(SLClosure Value) : SLObj<SLClosure>(Value)
{
    public override SLValue<SLClosure> Copy()
    {
        return new SLClosureObj(Value.Copy());
    }
    
    public override string ToString()
    {
        return Value.ToString();
    }
}

public record SLUpvalueObj(SLUpvalue Value) : SLObj<SLUpvalue>(Value)
{
    public override SLValue<SLUpvalue> Copy()
    {
        return new SLUpvalueObj(Value.Copy());
    }

    public override string ToString()
    {
        return "<upvalue>";
    }
}

public record SLNativeFuncObj(SLNativeFunction Value) : SLObj<SLNativeFunction>(Value)
{
    public override SLValue<SLNativeFunction> Copy()
    {
        return new SLNativeFuncObj(Value);
    }
    
    public override string ToString()
    {
        return Value.ToString();
    }
}

public record SLClassObj(SLClass Value) : SLObj<SLClass>(Value)
{
    public override SLValue<SLClass> Copy()
    {
        return new SLClassObj(Value.Copy());
    }
    
    public override string ToString()
    {
        return Value.ToString();
    }
}

public record SLClassInstanceObj(SLClassInstance Value) : SLObj<SLClassInstance>(Value)
{
    public override SLValue<SLClassInstance> Copy()
    {
        return new SLClassInstanceObj(Value.Copy());
    }
    
    public override string ToString()
    {
        return Value.ToString();
    }
}

public record SLBoundMethodObj(SLBoundMethod Value) : SLObj<SLBoundMethod>(Value)
{
    public override SLValue<SLBoundMethod> Copy()
    {
        return new SLBoundMethodObj(Value.Copy());
    }
    
    public override string ToString()
    {
        return Value.ToString();
    }
}

public record SLTableObj(SLTable Value) : SLObj<SLTable>(Value)
{
    public override SLValue<SLTable> Copy()
    {
        return new SLTableObj(Value.Copy());
    }
    
    public override string ToString()
    {
        return Value.ToString();
    }
}

public record SLArrayObj(SLArray Value) : SLObj<SLArray>(Value)
{
    public override SLValue<SLArray> Copy()
    {
        return new SLArrayObj(Value.Copy());
    }
    
    public override string ToString()
    {
        return Value.ToString();
    }
}