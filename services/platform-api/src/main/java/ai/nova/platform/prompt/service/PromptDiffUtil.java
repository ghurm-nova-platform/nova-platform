package ai.nova.platform.prompt.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.nova.platform.prompt.dto.PromptDtos.DiffLine;

final class PromptDiffUtil {

    private PromptDiffUtil() {
    }

    static List<DiffLine> lineDiff(String left, String right) {
        List<String> leftLines = splitLines(left);
        List<String> rightLines = splitLines(right);
        int[][] lcs = buildLcs(leftLines, rightLines);
        List<DiffLine> diff = new ArrayList<>();
        int i = leftLines.size();
        int j = rightLines.size();
        List<DiffLine> reversed = new ArrayList<>();

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && leftLines.get(i - 1).equals(rightLines.get(j - 1))) {
                reversed.add(new DiffLine("UNCHANGED", i, leftLines.get(i - 1)));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                reversed.add(new DiffLine("ADDED", j, rightLines.get(j - 1)));
                j--;
            } else {
                reversed.add(new DiffLine("REMOVED", i, leftLines.get(i - 1)));
                i--;
            }
        }

        for (int idx = reversed.size() - 1; idx >= 0; idx--) {
            diff.add(reversed.get(idx));
        }
        return diff;
    }

    private static List<String> splitLines(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(content.split("\\R", -1));
    }

    private static int[][] buildLcs(List<String> left, List<String> right) {
        int[][] dp = new int[left.size() + 1][right.size() + 1];
        for (int i = 1; i <= left.size(); i++) {
            for (int j = 1; j <= right.size(); j++) {
                if (left.get(i - 1).equals(right.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp;
    }
}
