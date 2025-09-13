package com.hrpd.codereview.service;

import com.hrpd.codereview.model.DiffHunk;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex-based unified diff splitter (good for demos).
 */
@Slf4j
public class DiffServiceImpl implements DiffService {

    private static final Pattern FILE_HEADER =
            Pattern.compile("^\\+\\+\\+\\s+b/(.+)$", Pattern.MULTILINE);
    private static final Pattern HUNK_HEADER =
            Pattern.compile("^@@\\s+\\-(\\d+),(\\d+)\\s+\\+(\\d+),(\\d+)\\s+@@.*$", Pattern.MULTILINE);

    @Override
    public List<DiffHunk> parseUnifiedPatch(String patch) {
        log.info("📄 Starting diff parsing - patch size: {} characters", patch.length());
        List<DiffHunk> hunks = new ArrayList<>();

        Matcher fileM = FILE_HEADER.matcher(patch);
        List<int[]> fileSpans = new ArrayList<>();
        while (fileM.find()) fileSpans.add(new int[]{fileM.start(), fileM.end()});
        
        log.info("📁 Found {} files in diff", fileSpans.size());

        for (int i = 0; i < fileSpans.size(); i++) {
            int start = fileSpans.get(i)[0];
            int end = (i + 1 < fileSpans.size()) ? fileSpans.get(i + 1)[0] : patch.length();
            String fileBlock = patch.substring(start, end);

            Matcher fileNameM = FILE_HEADER.matcher(fileBlock);
            String fileName = fileNameM.find() ? fileNameM.group(1) : "unknown";
            log.debug("📄 Processing file: {}", fileName);

            Matcher hunksM = HUNK_HEADER.matcher(fileBlock);
            List<int[]> hSpans = new ArrayList<>();
            while (hunksM.find()) hSpans.add(new int[]{hunksM.start(), hunksM.end()});

            if (hSpans.isEmpty()) {
                log.debug("⚠️ No hunks found in file: {}", fileName);
                continue;
            }
            
            log.debug("🔍 Found {} hunks in file: {}", hSpans.size(), fileName);

            for (int j = 0; j < hSpans.size(); j++) {
                int hs = hSpans.get(j)[0];
                int he = (j + 1 < hSpans.size()) ? hSpans.get(j + 1)[0] : fileBlock.length();
                String hunkText = fileBlock.substring(hs, he);
                hunks.add(new DiffHunk(fileName, 0, 0, hunkText));
                log.debug("✅ Added hunk {}/{} for file: {}", j + 1, hSpans.size(), fileName);
            }
        }

        if (hunks.isEmpty()) {
            log.warn("⚠️ No hunks parsed, creating fallback hunk");
            hunks.add(new DiffHunk("all", 0, 0, patch));
        }
        
        log.info("✅ Diff parsing complete: {} hunks from {} files", hunks.size(), fileSpans.size());
        return hunks;
    }
}
