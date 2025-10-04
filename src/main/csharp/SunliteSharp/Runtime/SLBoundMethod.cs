namespace SunliteSharp.Runtime;

public record SLBoundMethod(SLClosure Method, AnySLValue Receiver)
{
    public override string ToString()
    {
        return Method.ToString();
    }

    public SLBoundMethod Copy()
    {
        return new SLBoundMethod(Method.Copy(), Receiver.Copy());
    }
}