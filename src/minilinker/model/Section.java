package minilinker.model;

import minilinker.util.Messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Linker pipeline v2'de kullanılan section temsili.
 *
 * <p>Word listesi tutar ve 16-bit word doğrulaması yapar.</p>
 */
public class Section {

    private final SectionType type;
    private final List<Integer> words;

    public Section(SectionType type) {
        this.type = Objects.requireNonNull(type, "Section türü boş olamaz.");
        this.words = new ArrayList<>();
    }

    public Section(String name) {
        this(SectionType.fromName(name));
    }

    public SectionType getType() {
        return type;
    }

    public String getName() {
        return type.getName();
    }

    public List<Integer> getWords() {
        return Collections.unmodifiableList(words);
    }

    public int size() {
        return words.size();
    }

    public int getWordCount() {
        return words.size();
    }

    public boolean isEmpty() {
        return words.isEmpty();
    }

    public void addWord(int word) {
        validateWord(word);
        words.add(word);
    }

    public void addWords(List<Integer> wordList) {
        Objects.requireNonNull(wordList, "Word listesi boş olamaz.");

        for (Integer word : wordList) {
            if (word == null) {
                throw new IllegalArgumentException(Messages.ERR_WORD_EMPTY);
            }

            addWord(word);
        }
    }

    public int getWord(int index) {
        return words.get(index);
    }

    public void setWord(int index, int word) {
        validateWord(word);
        words.set(index, word);
    }

    private void validateWord(int word) {
        if (word < 0 || word > 0xFFFF) {
            throw new IllegalArgumentException(
                    "Word değeri 16-bit aralıkta olmalıdır: 0x0000 - 0xFFFF"
            );
        }
    }

    @Override
    public String toString() {
        return "Section{" +
                "name='" + getName() + '\'' +
                ", words=" + words +
                '}';
    }
}