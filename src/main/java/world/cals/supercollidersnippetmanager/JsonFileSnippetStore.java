package world.cals.supercollidersnippetmanager;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class JsonFileSnippetStore implements SnippetStore {
    private final Path dataDir;
    private final ObjectMapper om;

    public JsonFileSnippetStore(Path dataDir) {
        this.dataDir = dataDir;
        this.om = Json.mapper();
    }

    @Override
    public List<Snippet> loadAll() throws IOException {
        if (!Files.exists(dataDir)) {
            return List.of();
        }

        List<Snippet> out = new ArrayList<>();
        try (var stream = Files.walk(dataDir)) {
            stream
                    .filter(p -> Files.isRegularFile(p))
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            Snippet snippet = om.readValue(p.toFile(), Snippet.class);
                            out.add(snippet);
                        } catch (Exception e) {
                            System.err.println("Failed to read snippet JSON: " + p);
                            e.printStackTrace();
                        }
                    });
        }

        out.sort(Comparator.comparing(Snippet::getModifiedDate).reversed());
        return List.copyOf(out);
    }

    @Override
    public void createFolder(String folder) throws IOException {
        String safeFolder = sanitizeFolder(folder);
        Files.createDirectories(dataDir.resolve(safeFolder));
    }

    @Override
    public Snippet save(Snippet snippet) throws IOException {
        String folder = sanitizeFolder(snippet.getFolder());
        Path folderDir = dataDir.resolve(folder);
        Files.createDirectories(folderDir);

        Path file = folderDir.resolve(snippet.getId().toString() + ".json");

        Path tmp = Files.createTempFile(folderDir, snippet.getId().toString(), ".tmp");
        om.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), snippet);

        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }

        return snippet;
    }

    @Override
    public void delete(UUID id, String folder) throws IOException {
        String safeFolder = sanitizeFolder(folder);
        Path file = dataDir.resolve(safeFolder).resolve(id.toString() + ".json");
        Files.deleteIfExists(file);
    }

    private static String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            throw new IllegalArgumentException("folder is required");
        }
        Path p = Path.of(folder).normalize();

        if (p.isAbsolute()) {
            throw new IllegalArgumentException("folder must be relative");
        }
        for (Path part : p) {
            String s = part.toString();
            if (s.equals("..")) {
                throw new IllegalArgumentException("folder must not contain '..'");
            }
        }
        return p.toString().replace('\\', '/');
    }
}
