namespace SunliteSharp.Core;

public record TypeToken(Dictionary<Token, List<TypeToken>> Tokens, List<TypeToken> TypeParameters);