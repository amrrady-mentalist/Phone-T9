package com.arady.phone.t9

// The hard-coded priority list, copied verbatim (order matters) from the original web app.
// A word's weight is 100000 minus its first index in this list; every other word weighs 100.
object PriorityWords {
    val LIST: List<String> = listOf(
        "the", "of", "and", "a", "to", "in", "is", "you",
        "that", "it", "he", "was", "for", "on", "are", "as",
        "with", "his", "they", "i", "at", "be", "this", "have",
        "from", "or", "one", "had", "by", "word", "but", "not",
        "what", "all", "were", "we", "when", "your", "can", "said",
        "there", "use", "an", "each", "which", "she", "do", "how",
        "their", "if", "will", "up", "other", "about", "out", "many",
        "then", "them", "these", "so", "some", "her", "would", "make",
        "like", "him", "into", "time", "has", "look", "two", "more",
        "write", "go", "see", "number", "no", "way", "could", "people",
        "my", "than", "first", "water", "been", "call", "who", "oil",
        "its", "now", "find", "long", "down", "day", "did", "get",
        "come", "made", "may", "cat", "dog", "act", "bat", "home",
        "good", "nice", "hood", "power", "ropes", "arca", "cards", "magic",
        "coining", "shiner", "trick", "mental", "blind", "force", "psychic", "lemon",
        "alpses", "sex", "see", "sec", "ten", "tem", "ufo", "red",
        "bed", "match", "game", "test", "phone", "hello", "strong",
    )
}
