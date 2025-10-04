using System.Collections;

namespace SunliteSharp.Util;

public readonly record struct IntRange(int Start, int End) : IEnumerable<int>
{
    public bool Contains(int value) => value >= Start && value <= End;
    
    public IEnumerator<int> GetEnumerator()
    {
        for (var i = Start; i <= End; i++)
            yield return i;
    }

    public override string ToString()
    {
        return $"{Start}..{End}";
    }

    IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
}