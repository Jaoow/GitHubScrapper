package com.github.jaoow.scrapper;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Scrapper {

    public static final String TOKEN = "<your-token>";

    public static void main(String[] args) throws IOException {
        GitHub github = new GitHubBuilder().withOAuthToken(TOKEN).build();

        List<String> dependencies = new ArrayList<>();
        github.getMyself().getAllRepositories().forEach((s, ghRepository) -> {
            try {
                ghRepository.listReleases().forEach(ghRelease -> {
                    try {
                        ghRelease.getAssets().forEach(ghAsset -> {
                            dependencies.add(ghAsset.getBrowserDownloadUrl());
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        File libDir = new File(System.getenv("APPDATA"), "lib");
        loadDependencies(libDir, dependencies);
    }

    /**
     * Method to download all dependency from URL List
     *
     * @param libDir libs directory
     * @param dependencies dependency url list
     */
    public static void loadDependencies(File libDir, List<String> dependencies) throws RuntimeException {
        System.out.println(("Identified the following dependencies: " + dependencies.toString()));

        if (!(libDir.exists() || libDir.mkdirs())) {
            throw new RuntimeException("Unable to create lib dir - " + libDir.getPath());
        }

        dependencies.forEach(dependency -> {
            try {
                downloadDependency(libDir, dependency);
            } catch (Exception e) {
                System.out.println("Exception whilst downloading dependency " + dependency.substring(dependency.lastIndexOf('/') + 1));
            }
        });

    }

    /**
     * Method to download from a URL using Apache Https (without Java 11)
     *
     * @param libDir libs directory
     * @param dependency dependency url
     */
    private static void downloadDependencyApache(File libDir, String dependency) throws Exception {
        String fileName = dependency.substring(dependency.lastIndexOf('/') + 1);

        File file = new File(libDir, fileName);
        if (file.exists()) {
            return;
        }

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(dependency);
        request.addHeader("Authorization", "token " + TOKEN);

        HttpEntity entity = client.execute(request).getEntity();

        System.out.println("Dependency '" + fileName + "' could not be found. Attempting to download.");
        try (InputStream in = entity.getContent()) {
            Files.copy(in, file.toPath());
        }

        if (!file.exists()) {
            throw new IllegalStateException("File not present. - " + file.toString());
        } else {
            System.out.println("Dependency '" + fileName + "' successfully downloaded.");
        }
    }


    /**
     * Method to download from a URL using Java 11
     *
     * @param libDir libs directory
     * @param dependency dependency url
     */
    private static void downloadDependency(File libDir, String dependency) throws Exception {
        String fileName = dependency.substring(dependency.lastIndexOf('/') + 1);

        File file = new File(libDir, fileName);
        if (file.exists()) {
            return;
        }

        // create client

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "token " + TOKEN)
                .uri(URI.create(dependency))
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());


        System.out.println("Dependency '" + fileName + "' could not be found. Attempting to download.");
        try (InputStream in = response.body()) {
            Files.copy(in, file.toPath());
        }

        if (!file.exists()) {
            throw new IllegalStateException("File not present. - " + file.toString());
        } else {
            System.out.println("Dependency '" + fileName + "' successfully downloaded.");
        }
    }
}
