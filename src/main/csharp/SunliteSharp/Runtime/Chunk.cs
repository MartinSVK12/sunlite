using System.Collections.Immutable;
using System.Text;
using SunliteSharp.Util;

namespace SunliteSharp.Runtime;

public record ChunkHeader(int[] Lines, string File, string Name = "<script>");

public record MutableChunkHeader(List<int> Lines, string file, string name = "<script>")
{
    public ChunkHeader ToImmutable()
    {
        return new ChunkHeader(Lines.ToArray(), File, Name);
    }

    public string File { get; set; } = file;
    public string Name { get; set; } = name;
}

public record Chunk(
    byte[] Code,
    ImmutableDictionary<IntRange, IntRange> Exceptions,
    AnySLValue[] Constants,
    ChunkHeader Header)
{
    public int Length()
    {
        return Code.Length;
    }

    public override string ToString()
    {
        var sb = new StringBuilder();
        sb.Append($"==== {Header.File}::{Header.Name} ====\n");
        foreach (var (index, b) in Code.Select((value, i) => (i, value)))
        {
            sb.Append($"{index:X4}: {b:X2}\n");
        }
        var repeatCount = (Header.File.Length) + Header.Name.Length;
        sb.Append($"====={new string('=', repeatCount)}=====\n");
        return sb.ToString();
    }
}

public record MutableChunk(
    List<byte> Code,
    Dictionary<IntRange, IntRange> Exceptions,
    List<AnySLValue> Constants,
    MutableChunkHeader Header)
{
    public int Length()
    {
        return Code.Count;
    }
    
    public Chunk ToImmutable()
    {
        return new Chunk(Code.ToArray(), Exceptions.ToImmutableDictionary(), Constants.ToArray(), Header.ToImmutable());
    }
    
    public override string ToString()
    {
        var sb = new StringBuilder();
        sb.Append($"==== {Header.File}::{Header.Name} (mutable) ====\n");
        foreach (var (index, b) in Code.Select((value, i) => (i, value)))
        {
            sb.Append($"{index:X4}: {b:X2}\n");
        }
        var repeatCount = (Header.File.Length) + Header.Name.Length;
        sb.Append($"====={new string('=', repeatCount)}=====\n");
        return sb.ToString();
    }
}