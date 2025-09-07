package com.hrpd.codereview.reviewer;

import com.hrpd.codereview.model.DiffHunk;
import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.model.ReviewerType;

import java.util.List;

/**
 * Contract for a reviewer persona.
 */
public interface Reviewer {
    ReviewerType type();
    ReviewResult review(List<DiffHunk> hunks);
}
