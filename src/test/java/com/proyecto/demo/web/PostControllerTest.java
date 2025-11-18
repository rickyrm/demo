package com.proyecto.demo.web;

import com.proyecto.demo.service.PostService;
import com.proyecto.demo.util.SlugUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PostControllerTest {

    private PostService postService;
    private com.proyecto.demo.repository.InMemorySlugRedirectRepository redirectRepository;
    private PostController controller;

    private final Path postsDir = Paths.get("content/posts");

    @BeforeEach
    void setUp() {
        postService = Mockito.mock(PostService.class);
        redirectRepository = new com.proyecto.demo.repository.InMemorySlugRedirectRepository();
        controller = new PostController(postService, redirectRepository);
        // ensure clean workspace
        try {
            if (Files.exists(postsDir)) {
                if (Files.isDirectory(postsDir)) {
                    Files.walk(postsDir)
                            .map(Path::toFile)
                            .sorted((a, b) -> -a.compareTo(b))
                            .forEach(f -> f.delete());
                    Files.deleteIfExists(postsDir);
                } else {
                    Files.deleteIfExists(postsDir);
                }
            }
        } catch (Exception e) {
            // ignore cleanup errors
        }
    }

    @AfterEach
    void tearDown() {
        try {
            if (Files.exists(postsDir)) {
                if (Files.isDirectory(postsDir)) {
                    Files.walk(postsDir)
                            .map(Path::toFile)
                            .sorted((a, b) -> -a.compareTo(b))
                            .forEach(f -> f.delete());
                    Files.deleteIfExists(postsDir);
                } else {
                    Files.deleteIfExists(postsDir);
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    void slugifyEndpoint_returnsGeneratedSlug() {
        Map<String, String> req = Map.of("title", "Mi Título con ñ y acentos!!!");
        var resp = controller.slugifyTitle(req);
        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        String expected = SlugUtil.slugify(req.get("title"));
        assertEquals(expected, resp.getBody().get("slug"));
    }

    @Test
    void createPost_writesMarkdownFile_andReturns201() throws Exception {
        String title = "Una entrada de prueba";
        String content = "# Hola\nContenido";
        Map<String, Object> req = Map.of("title", title, "content", content, "metaDescription", "meta prueba");

        var resp = controller.createPost(req);
        assertEquals(201, resp.getStatusCodeValue());
        Object body = resp.getBody();
        assertTrue(body instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) body;
        assertTrue(map.containsKey("slug"));
        assertTrue(map.containsKey("file"));
        Path file = Paths.get((String) map.get("file"));
        assertTrue(Files.exists(file), "Markdown file should exist");
        String fileContent = Files.readString(file);
        assertTrue(fileContent.contains("title: \"" + title.replace("\"", "'") + "\""));
        assertTrue(fileContent.contains(content));
    }

    @Test
    void createPost_whenFileWriteFails_rollsBackAndReturns500() throws Exception {
        String title = "Entrada con fallo";
        String content = "contenido";
        Map<String, Object> req = Map.of("title", title, "content", content);

        // create a regular file at path 'content/posts' so createDirectories fails
        Path postsPath = Paths.get("content/posts");
        Files.createDirectories(postsPath.getParent());
        Files.writeString(postsPath, "I am a file, not a dir");

        var resp = controller.createPost(req);
        assertEquals(500, resp.getStatusCodeValue());

        // ensure rollback attempted
        Mockito.verify(postService).deletePostById(ArgumentMatchers.any(UUID.class));
    }

}
