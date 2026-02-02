package world.cals.supercollidersnippetmanager;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Snippet {
    private final UUID id;
    private final String name;
    private final String description;
    private final String code;
    private final List<String> tags;
    private final String folder;
    private final Instant createdDate;
    private final Instant modifiedDate;

    @JsonCreator
    public Snippet(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("code") String code,
            @JsonProperty("tags") List<String> tags,
            @JsonProperty("folder") String folder,
            @JsonProperty("createdDate") Instant createdDate,
            @JsonProperty("modifiedDate") Instant modifiedDate
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireNonBlank(name, "name");
        this.code = requireNonBlank(code, "code");
        this.folder = requireNonBlank(folder, "folder");
        this.description = description;
        this.tags = (tags == null) ? List.of() : List.copyOf(tags);
        this.createdDate = Objects.requireNonNull(createdDate, "createdDate");
        this.modifiedDate = Objects.requireNonNull(modifiedDate, "modifiedDate");
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCode() { return code; }
    public List<String> getTags() { return tags; }
    public String getFolder() { return folder; }
    public Instant getCreatedDate() { return createdDate; }
    public Instant getModifiedDate() { return modifiedDate; }

    public Snippet withUpdatedContent(String name, String description, String code, List<String> tags, String folder) {
        Instant now = Instant.now();
        return new Snippet(
                this.id,
                requireNonBlank(name, "name"),
                description,
                requireNonBlank(code, "code"),
                (tags == null) ? List.of() : List.copyOf(tags),
                requireNonBlank(folder, "folder"),
                this.createdDate,
                now
        );
    }

    public static Snippet createNew(String name, String description, String code, List<String> tags, String folder) {
        Instant now = Instant.now();
        return new Snippet(
                UUID.randomUUID(),
                name,
                description,
                code,
                tags,
                folder,
                now,
                now
        );
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    @Override
    public String toString() {
        return name;
    }
}
