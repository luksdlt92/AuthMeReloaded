package messages;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verifies a message file's keys to ensure that it is in sync with {@link MessageKey}, i.e. that the file contains
 * all keys and that it doesn't have any unknown ones.
 */
public class MessageFileVerifier {

    private final String messagesFile;
    private final Set<String> unknownKeys = new HashSet<>();
    // Map with the missing key and a boolean indicating whether or not it was added to the file by this object
    private final Map<String, Boolean> missingKeys = new HashMap<>();
    private final Multimap<String, String> missingTags = HashMultimap.create();

    /**
     * Create a verifier that verifies the given messages file.
     *
     * @param messagesFile The messages file to process
     */
    public MessageFileVerifier(String messagesFile) {
        this.messagesFile = messagesFile;
        verifyKeys();
    }

    /**
     * Return the list of unknown keys, i.e. the list of keys present in the file that are not
     * part of the {@link MessageKey} enum.
     *
     * @return List of unknown keys
     */
    public Set<String> getUnknownKeys() {
        return unknownKeys;
    }

    /**
     * Return the list of missing keys, i.e. all keys that are part of {@link MessageKey} but absent
     * in the messages file.
     *
     * @return The list of missing keys in the file
     */
    public Map<String, Boolean> getMissingKeys() {
        return missingKeys;
    }

    /**
     * Return the collection of tags the message key defines that aren't present in the read line.
     *
     * @return Collection of missing tags per message key. Key = message key, value = missing tag.
     */
    public Multimap<String, String> getMissingTags() {
        return missingTags;
    }

    private void verifyKeys() {
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(new File(messagesFile));

        // Check known keys (their existence + presence of all tags)
        for (MessageKey messageKey : MessageKey.values()) {
            final String key = messageKey.getKey();
            if (configuration.isString(key)) {
                checkTagsInMessage(messageKey, configuration.getString(key));
            } else {
                missingKeys.put(key, false);
            }
        }

        // Check FileConfiguration for all of its keys to find unknown keys
        for (String key : configuration.getValues(true).keySet()) {
            if (!messageKeyExists(key)) {
                unknownKeys.add(key);
            }
        }
    }

    private void checkTagsInMessage(MessageKey messageKey, String message) {
        for (String tag : messageKey.getTags()) {
            if (!message.contains(tag)) {
                missingTags.put(messageKey.getKey(), tag);
            }
        }
    }

    /**
     * Add missing keys to the file with the provided default (English) message.
     *
     * @param defaultMessages The collection of default messages
     */
    public void addMissingKeys(FileConfiguration defaultMessages) {
        final List<String> fileLines = new ArrayList<>(
            Arrays.asList(FileUtils.readFromFile(messagesFile).split("\\n")));

        List<String> keysToAdd = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : missingKeys.entrySet()) {
            final String key = entry.getKey();

            if (Boolean.FALSE.equals(entry.getValue()) && defaultMessages.get(key) != null) {
                keysToAdd.add(key);
            }
        }

        // Add missing keys as comments to the bottom of the file
        for (String keyToAdd : keysToAdd) {
            int indexOfComment = Iterables.indexOf(fileLines, isCommentFor(keyToAdd));
            if (indexOfComment != -1) {
                // Comment for keyToAdd already exists, so remove it since we're going to add it
                fileLines.remove(indexOfComment);
            }
            String comment = commentForKey(keyToAdd) + "'" +
                defaultMessages.getString(keyToAdd).replace("'", "''") + "'";
            fileLines.add(comment);
            missingKeys.put(keyToAdd, Boolean.TRUE);
        }

        // Add a comment above messages missing a tag
        for (Map.Entry<String, Collection<String>> entry : missingTags.asMap().entrySet()) {
            final String key = entry.getKey();
            addCommentForMissingTags(fileLines, key, entry.getValue());
        }

        FileUtils.writeToFile(messagesFile, StringUtils.join("\n", fileLines));
    }

    /**
     * Add a comment above a message to note the tags the message is missing. Removes
     * any similar comment that may already be above the message.
     *
     * @param fileLines The lines of the file (to modify)
     * @param key The key of the message
     * @param tags The missing tags
     */
    private void addCommentForMissingTags(List<String> fileLines, final String key, Collection<String> tags) {
        int indexForComment = Iterables.indexOf(fileLines, isCommentFor(key));
        if (indexForComment == -1) {
            indexForComment = Iterables.indexOf(fileLines, new Predicate<String>() {
                @Override
                public boolean apply(String input) {
                    return input.startsWith(key + ": ");
                }
            });
            if (indexForComment == -1) {
                System.err.println("Error adding comment for key '" + key + "': couldn't find entry in file lines");
                return;
            }
        } else {
            fileLines.remove(indexForComment);
        }

        String tagWord = tags.size() > 1 ? "tags" : "tag";
        fileLines.add(indexForComment, commentForKey(key)
            + String.format("Missing %s %s", tagWord, StringUtils.join(", ", tags)));
    }

    private static String commentForKey(String key) {
        return String.format("# TODO %s: ", key);
    }

    private static Predicate<String> isCommentFor(final String key) {
        return new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input.startsWith(commentForKey(key));
            }
        };
    }

    private static boolean messageKeyExists(String key) {
        for (MessageKey messageKey : MessageKey.values()) {
            if (messageKey.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }
}
