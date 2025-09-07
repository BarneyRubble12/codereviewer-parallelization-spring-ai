package com.hrpd.codereview.service;

import java.io.IOException;

/**
 * Fetches PR patches/diffs from GitHub REST.
 */
public interface GithubClientService {
    String fetchPrPatch(String repo, int prNumber) throws IOException, InterruptedException;
    String fetchPrDiff(String repo, int prNumber) throws IOException, InterruptedException;
}
