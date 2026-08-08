package minilinker.model;

import minilinker.architecture.Mini16Architecture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Bir MINIOBJ dosyasındaki .text veya .data section'ı. */
public final class ObjectSection {

    private final SectionType type;
    private final List<Integer> words;

    public ObjectSection(SectionType type) {
        this.type = Objects.requireNonNull(
                type,
                "Section türü boş olamaz"
        );
        this.words = new ArrayList<>();
    }

    public SectionType getType() {
        return type;
    }

    public void addWord(int word) {
        Mini16Architecture.validateWord(word);
        words.add(word);
    }

    public void addWords(Iterable<Integer> newWords) {
        Objects.requireNonNull(
                newWords,
                "Word listesi boş olamaz"
        );

        for (Integer word : newWords) {
            if (word == null) {
                throw new IllegalArgumentException(
                        "Word değeri boş olamaz"
                );
            }

            addWord(word);
        }
    }

    public int wordCount() {
        return words.size();
    }

    public int getWord(int offset) {
        validateOffset(offset);
        return words.get(offset);
    }

    public void setWord(int offset, int word) {
        validateOffset(offset);
        Mini16Architecture.validateWord(word);
        words.set(offset, word);
    }

    public List<Integer> getWords() {
        return Collections.unmodifiableList(words);
    }

    public String toHexWords() {
        StringBuilder result = new StringBuilder();

        for (int index = 0; index < words.size(); index++) {
            if (index > 0) {
                result.append(' ');
            }

            result.append(
                    Mini16Architecture.formatWord(words.get(index))
            );
        }

        return result.toString();
    }

    private void validateOffset(int offset) {
        if (offset < 0 || offset >= words.size()) {
            throw new IndexOutOfBoundsException(
                    "Section word offset'i geçersiz: " + offset
            );
        }
    }
}