import app.astrid.engine.ExerciseInspector;
import app.astrid.engine.GeneratedExercise;
import app.astrid.engine.LexicalTarget;
import app.astrid.engine.PracticePlan;
import app.astrid.engine.PracticePlanItem;
import app.astrid.engine.PracticePlanner;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeMap;

/** Deterministic, private audit of practice-1 plan variation. */
public final class PracticeVariationAudit {
    private static final String FOCUS = "reading-basics";
    private static final String PLAN_REVISION = "practice-1";
    private static final int SAMPLE_SIZE = 4096;
    private static final long RANDOM_STREAM_SEED = 0x415354524944L;
    private static final long NONNEGATIVE_INT_BOUND = 1L << 31;
    private static final List<String> SENSES = List.of("L014-S01", "L015-S01", "L018-S01");
    private static final List<String> EXPECTED_FORMATS =
            List.of("E007", "E003", "E007", "E008", "E007", "E008", "E006", "E008");
    private static final List<String> EXPECTED_SKILLS =
            Arrays.asList(null, "G005", null, null, null, null, "G006", null);

    private PracticeVariationAudit() {}

    public static void main(String[] args) throws Exception {
        Path output = args.length == 0 ? Path.of("variation-audit.json") : Path.of(args[0]);
        PracticePlanner planner = new PracticePlanner();

        int[] sequential = new int[SAMPLE_SIZE];
        for (int i = 0; i < SAMPLE_SIZE; i++) sequential[i] = i;
        int[] pseudorandom = new int[SAMPLE_SIZE];
        SplittableRandom random = new SplittableRandom(RANDOM_STREAM_SEED);
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            pseudorandom[i] = (int) random.nextLong(NONNEGATIVE_INT_BOUND);
        }

        CohortResult sequentialResult = audit(planner, "sequential", sequential, true);
        CohortResult randomResult = audit(planner, "pseudorandom", pseudorandom, false);
        String json = renderRoot(sequentialResult, randomResult);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, json, StandardCharsets.UTF_8);
    }

    private static CohortResult audit(
            PracticePlanner planner, String name, int[] seeds, boolean adjacentSeeds) {
        CohortResult result = new CohortResult(name, seeds.length);
        String previousSemantic = null;
        String previousPresentation = null;
        int previousSeed = -1;
        for (int seed : seeds) {
            PracticePlan plan = planner.plan(FOCUS, PLAN_REVISION, seed);
            String semantic = semanticPlanKey(plan);
            String presentation = presentationPlanKey(plan);
            result.semanticKeys.add(semantic);
            result.presentationKeys.add(presentation);
            result.seedValues.add(seed);
            if (adjacentSeeds && previousSemantic != null && seed == previousSeed + 1) {
                if (semantic.equals(previousSemantic)) result.adjacentSemanticRepeats++;
                if (presentation.equals(previousPresentation)) result.adjacentPresentationRepeats++;
            }
            previousSemantic = semantic;
            previousPresentation = presentation;
            previousSeed = seed;
            collectDistributions(result, plan);
            checkInvariants(result, plan, seed);
        }
        return result;
    }

    private static void collectDistributions(CohortResult result, PracticePlan plan) {
        for (PracticePlanItem item : plan.getItems()) {
            GeneratedExercise exercise = item.getExercise();
            int position = item.getPosition();
            PositionDistribution distribution = result.positions.get(position);
            increment(distribution.targets, targetKey(exercise));
            increment(distribution.nouns, nounEntryId(exercise));
            increment(distribution.subjects, subjectEntryId(exercise));
            if (position == 1 || position == 6) {
                String canonicalId = valueOrNone(exercise.getInspector().getCanonicalSentenceId());
                increment(result.grammarCanonicalIds.get(position), canonicalId);
            }
        }
    }

    private static void checkInvariants(CohortResult result, PracticePlan plan, int requestedSeed) {
        List<PracticePlanItem> items = plan.getItems();
        boolean identity = FOCUS.equals(plan.getFocusId()) && PLAN_REVISION.equals(plan.getPlanRevision())
                && plan.getSeed() == requestedSeed && "Words and simple statements".equals(plan.getTitle());
        result.record("plan_identity", identity, requestedSeed);

        boolean revisions = "pilot-3".equals(plan.getContentRevision())
                && "generator-3".equals(plan.getGeneratorRevision())
                && "evaluation-3".equals(plan.getEvaluationRevision());
        result.record("pinned_revisions", revisions, requestedSeed);

        boolean layout = items.size() == 8;
        if (layout) {
            for (int i = 0; i < 8; i++) {
                GeneratedExercise exercise = items.get(i).getExercise();
                layout &= items.get(i).getPosition() == i;
                layout &= EXPECTED_FORMATS.get(i).equals(exercise.getFormatId());
                layout &= java.util.Objects.equals(EXPECTED_SKILLS.get(i), exercise.getTargetSkillId());
            }
        }
        result.record("eight_item_format_target_layout", layout, requestedSeed);

        Set<String> recognitionSenses = new LinkedHashSet<>();
        Set<String> recallSenses = new LinkedHashSet<>();
        for (PracticePlanItem item : items) {
            GeneratedExercise exercise = item.getExercise();
            LexicalTarget lexical = exercise.getLexicalTarget();
            if (lexical != null && "E007".equals(exercise.getFormatId()) && "da-en".equals(lexical.getDirection())) {
                recognitionSenses.add(lexical.getSenseId());
            }
            if (lexical != null && "E008".equals(exercise.getFormatId()) && "en-da".equals(lexical.getDirection())) {
                recallSenses.add(lexical.getSenseId());
            }
        }
        result.record("recognition_covers_all_three_senses",
                recognitionSenses.equals(new LinkedHashSet<>(SENSES)), requestedSeed);
        result.record("recall_covers_all_three_senses",
                recallSenses.equals(new LinkedHashSet<>(SENSES)), requestedSeed);

        boolean grammar = items.size() == 8;
        if (grammar) {
            GeneratedExercise ordering = items.get(1).getExercise();
            GeneratedExercise repair = items.get(6).getExercise();
            grammar = "G005".equals(ordering.getTargetSkillId())
                    && "G006".equals(repair.getTargetSkillId())
                    && ordering.getInspector().getCanonicalSentenceId() != null
                    && repair.getInspector().getCanonicalSentenceId() != null
                    && ordering.getInspector().getLexicalIds().contains("L011")
                    && repair.getInspector().getLexicalIds().contains("L011")
                    && nounEntryId(ordering).equals(nounEntryId(items.get(4).getExercise()))
                    && nounEntryId(repair).equals(nounEntryId(items.get(0).getExercise()));
        }
        result.record("licensed_grammar_canonical_pairing", grammar, requestedSeed);

        boolean noAdjacentNouns = items.size() == 8;
        for (int i = 1; noAdjacentNouns && i < items.size(); i++) {
            noAdjacentNouns = !nounEntryId(items.get(i - 1).getExercise())
                    .equals(nounEntryId(items.get(i).getExercise()));
        }
        result.record("no_adjacent_noun_repeat", noAdjacentNouns, requestedSeed);

        boolean recognitionBeforeRecall = true;
        for (String sense : SENSES) {
            int recognition = positionOf(items, "E007", sense);
            int recall = positionOf(items, "E008", sense);
            recognitionBeforeRecall &= recognition >= 0 && recall >= 0 && recognition < recall;
        }
        result.record("recognition_precedes_matching_recall", recognitionBeforeRecall, requestedSeed);
    }

    private static int positionOf(List<PracticePlanItem> items, String format, String sense) {
        for (PracticePlanItem item : items) {
            LexicalTarget target = item.getExercise().getLexicalTarget();
            if (format.equals(item.getExercise().getFormatId()) && target != null
                    && sense.equals(target.getSenseId())) return item.getPosition();
        }
        return -1;
    }

    private static String semanticPlanKey(PracticePlan plan) {
        StringBuilder out = new StringBuilder();
        for (PracticePlanItem item : plan.getItems()) {
            GeneratedExercise exercise = item.getExercise();
            ExerciseInspector inspector = exercise.getInspector();
            out.append(item.getPosition()).append('|')
                    .append(exercise.getFormatId()).append('|')
                    .append(targetKey(exercise)).append('|')
                    .append(String.join(",", inspector.getLexicalIds())).append('|')
                    .append(String.join(",", inspector.getClauseRoles())).append('\n');
        }
        return out.toString();
    }

    private static String presentationPlanKey(PracticePlan plan) {
        StringBuilder out = new StringBuilder();
        for (PracticePlanItem item : plan.getItems()) {
            out.append(item.getPosition()).append('|')
                    .append(item.getExercise().getPresentation().toString()).append('\n');
        }
        return out.toString();
    }

    private static String targetKey(GeneratedExercise exercise) {
        if (exercise.getTargetSkillId() != null) return "skill:" + exercise.getTargetSkillId();
        LexicalTarget target = exercise.getLexicalTarget();
        if (target == null) return "none";
        return "lexical:" + target.getEntryId() + ":" + target.getSenseId() + ":"
                + target.getFacet() + ":" + target.getDirection();
    }

    private static String nounEntryId(GeneratedExercise exercise) {
        if (exercise.getLexicalTarget() != null) return exercise.getLexicalTarget().getEntryId();
        for (String id : exercise.getInspector().getLexicalIds()) {
            if (id.equals("L014") || id.equals("L015") || id.equals("L018")) return id;
        }
        return "none";
    }

    private static String subjectEntryId(GeneratedExercise exercise) {
        for (String id : exercise.getInspector().getLexicalIds()) {
            if (id.matches("L00[1-8]")) return id;
        }
        return "none";
    }

    private static String valueOrNone(String value) {
        return value == null ? "none" : value;
    }

    private static void increment(Map<String, Integer> counts, String key) {
        counts.merge(key, 1, Integer::sum);
    }

    private static String renderRoot(CohortResult sequential, CohortResult pseudorandom) {
        StringBuilder json = new StringBuilder(64_000);
        json.append("{\n");
        field(json, 1, "schemaVersion", "practice-variation-audit-v1", true);
        field(json, 1, "focusId", FOCUS, true);
        field(json, 1, "planRevision", PLAN_REVISION, true);
        json.append(indent(1)).append("\"semanticKeyDefinition\": ")
                .append(quote("ordered items: position + formatId + target(skill or lexical entry/sense/facet/direction) + inspector.lexicalIds + inspector.clauseRoles; excludes exercise/reproduction IDs and seed"))
                .append(",\n");
        json.append(indent(1)).append("\"presentationKeyDefinition\": ")
                .append(quote("ordered item position + GeneratedExercise.presentation.toString(); excludes request/reproduction metadata"))
                .append(",\n");
        json.append(indent(1)).append("\"pseudorandomSeedMethod\": {\n");
        field(json, 2, "generator", "java.util.SplittableRandom", true);
        field(json, 2, "streamSeedDecimal", Long.toString(RANDOM_STREAM_SEED), true);
        field(json, 2, "streamSeedHex", "0x415354524944", true);
        field(json, 2, "draw", "(int) nextLong(2147483648L), preserving draw order and duplicates", false);
        json.append(indent(1)).append("},\n");
        Set<Integer> combinedSeeds = new LinkedHashSet<>(sequential.seedValues);
        combinedSeeds.addAll(pseudorandom.seedValues);
        Set<String> combinedSemantic = new LinkedHashSet<>(sequential.semanticKeys);
        combinedSemantic.addAll(pseudorandom.semanticKeys);
        Set<String> combinedPresentations = new LinkedHashSet<>(sequential.presentationKeys);
        combinedPresentations.addAll(pseudorandom.presentationKeys);
        json.append(indent(1)).append("\"combined\": {\n");
        numberField(json, 2, "sampleCount", sequential.sampleCount + pseudorandom.sampleCount, true);
        numberField(json, 2, "distinctSeedCount", combinedSeeds.size(), true);
        numberField(json, 2, "distinctSemanticPlans", combinedSemantic.size(), true);
        numberField(json, 2, "distinctPresentations", combinedPresentations.size(), true);
        numberField(json, 2, "semanticPlansSeenInBothCohorts",
                intersectionSize(sequential.semanticKeys, pseudorandom.semanticKeys), true);
        numberField(json, 2, "presentationsSeenInBothCohorts",
                intersectionSize(sequential.presentationKeys, pseudorandom.presentationKeys), false);
        json.append(indent(1)).append("},\n");
        json.append(indent(1)).append("\"cohorts\": [\n");
        renderCohort(json, sequential, 2);
        json.append(",\n");
        renderCohort(json, pseudorandom, 2);
        json.append("\n").append(indent(1)).append("]\n}").append('\n');
        return json.toString();
    }

    private static <T> int intersectionSize(Set<T> left, Set<T> right) {
        Set<T> intersection = new LinkedHashSet<>(left);
        intersection.retainAll(right);
        return intersection.size();
    }

    private static void renderCohort(StringBuilder json, CohortResult result, int depth) {
        json.append(indent(depth)).append("{\n");
        field(json, depth + 1, "name", result.name, true);
        numberField(json, depth + 1, "sampleCount", result.sampleCount, true);
        numberField(json, depth + 1, "distinctSeedCount", result.seedValues.size(), true);
        numberField(json, depth + 1, "distinctSemanticPlans", result.semanticKeys.size(), true);
        numberField(json, depth + 1, "distinctPresentations", result.presentationKeys.size(), true);
        if ("sequential".equals(result.name)) {
            numberField(json, depth + 1, "adjacentSeedPairCount", result.sampleCount - 1, true);
            numberField(json, depth + 1, "exactSemanticRepeatsAcrossAdjacentSeeds", result.adjacentSemanticRepeats, true);
            numberField(json, depth + 1, "exactPresentationRepeatsAcrossAdjacentSeeds", result.adjacentPresentationRepeats, true);
        }
        json.append(indent(depth + 1)).append("\"invariants\": ");
        renderInvariants(json, result.invariants, depth + 1);
        json.append(",\n");
        json.append(indent(depth + 1)).append("\"perPositionDistributions\": [\n");
        for (int i = 0; i < result.positions.size(); i++) {
            if (i > 0) json.append(",\n");
            PositionDistribution p = result.positions.get(i);
            json.append(indent(depth + 2)).append("{\n");
            numberField(json, depth + 3, "position", i, true);
            mapField(json, depth + 3, "targets", p.targets, true);
            mapField(json, depth + 3, "nouns", p.nouns, true);
            mapField(json, depth + 3, "subjects", p.subjects, false);
            json.append(indent(depth + 2)).append("}");
        }
        json.append("\n").append(indent(depth + 1)).append("],\n");
        json.append(indent(depth + 1)).append("\"grammarCanonicalCoverage\": {\n");
        int emitted = 0;
        for (Map.Entry<Integer, Map<String, Integer>> entry : result.grammarCanonicalIds.entrySet()) {
            if (emitted++ > 0) json.append(",\n");
            json.append(indent(depth + 2)).append(quote("position" + entry.getKey())).append(": {\n");
            numberField(json, depth + 3, "distinctCanonicalIds", entry.getValue().size(), true);
            mapField(json, depth + 3, "canonicalIdCounts", entry.getValue(), false);
            json.append(indent(depth + 2)).append("}");
        }
        json.append("\n").append(indent(depth + 1)).append("}\n");
        json.append(indent(depth)).append("}");
    }

    private static void renderInvariants(
            StringBuilder json, Map<String, InvariantResult> invariants, int depth) {
        json.append("{\n");
        int emitted = 0;
        for (Map.Entry<String, InvariantResult> entry : invariants.entrySet()) {
            if (emitted++ > 0) json.append(",\n");
            InvariantResult value = entry.getValue();
            json.append(indent(depth + 1)).append(quote(entry.getKey())).append(": {\n");
            numberField(json, depth + 2, "checkedPlans", value.checked, true);
            numberField(json, depth + 2, "violationCount", value.violations, true);
            json.append(indent(depth + 2)).append("\"firstViolationSeeds\": ");
            intList(json, value.firstViolationSeeds);
            json.append('\n').append(indent(depth + 1)).append("}");
        }
        json.append('\n').append(indent(depth)).append("}");
    }

    private static void mapField(
            StringBuilder json, int depth, String name, Map<String, Integer> values, boolean comma) {
        json.append(indent(depth)).append(quote(name)).append(": {");
        int emitted = 0;
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (emitted++ > 0) json.append(", ");
            json.append(quote(entry.getKey())).append(": ").append(entry.getValue());
        }
        json.append("}").append(comma ? "," : "").append('\n');
    }

    private static void field(
            StringBuilder json, int depth, String name, String value, boolean comma) {
        json.append(indent(depth)).append(quote(name)).append(": ").append(quote(value))
                .append(comma ? "," : "").append('\n');
    }

    private static void numberField(
            StringBuilder json, int depth, String name, int value, boolean comma) {
        json.append(indent(depth)).append(quote(name)).append(": ").append(value)
                .append(comma ? "," : "").append('\n');
    }

    private static void intList(StringBuilder json, List<Integer> values) {
        json.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(values.get(i));
        }
        json.append(']');
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    private static String indent(int depth) {
        return "  ".repeat(depth);
    }

    private static final class PositionDistribution {
        final Map<String, Integer> targets = new TreeMap<>();
        final Map<String, Integer> nouns = new TreeMap<>();
        final Map<String, Integer> subjects = new TreeMap<>();
    }

    private static final class InvariantResult {
        int checked;
        int violations;
        final List<Integer> firstViolationSeeds = new ArrayList<>();
    }

    private static final class CohortResult {
        final String name;
        final int sampleCount;
        final Set<Integer> seedValues = new LinkedHashSet<>();
        final Set<String> semanticKeys = new LinkedHashSet<>();
        final Set<String> presentationKeys = new LinkedHashSet<>();
        final List<PositionDistribution> positions = new ArrayList<>();
        final Map<Integer, Map<String, Integer>> grammarCanonicalIds = new LinkedHashMap<>();
        final Map<String, InvariantResult> invariants = new LinkedHashMap<>();
        int adjacentSemanticRepeats;
        int adjacentPresentationRepeats;

        CohortResult(String name, int sampleCount) {
            this.name = name;
            this.sampleCount = sampleCount;
            for (int i = 0; i < 8; i++) positions.add(new PositionDistribution());
            grammarCanonicalIds.put(1, new TreeMap<>());
            grammarCanonicalIds.put(6, new TreeMap<>());
        }

        void record(String name, boolean passed, int seed) {
            InvariantResult invariant = invariants.computeIfAbsent(name, ignored -> new InvariantResult());
            invariant.checked++;
            if (!passed) {
                invariant.violations++;
                if (invariant.firstViolationSeeds.size() < 10) invariant.firstViolationSeeds.add(seed);
            }
        }
    }
}
