package world.cals.supercollidersnippetmanager;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface SnippetStore {
    List<Snippet> loadAll() throws IOException;

    Snippet save(Snippet snippet) throws IOException;

    void delete(UUID id, String folder) throws IOException;

    void createFolder(String folder) throws IOException;
}
