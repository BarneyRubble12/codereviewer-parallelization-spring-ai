package com.hrpd.codereview.service;

import com.hrpd.codereview.model.DiffHunk;

import java.util.List;

public interface DiffService {

    /**
     * Splits a unified patch into focused hunks to keep prompts concise.
     *
     * @param patch unified diff text.
     * @return list of {@link DiffHunk}s.
     */
    List<DiffHunk> parseUnifiedPatch(String patch);

}
