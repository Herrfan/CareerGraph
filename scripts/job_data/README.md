# Job Data Preprocessing

## Purpose

This folder is for offline preprocessing only.

The expected workflow is:

1. keep raw Excel or crawler output in a raw data folder
2. run rule-based preprocessing
3. produce normalized `jsonl`
4. import normalized data into MySQL and Neo4j

## Output Files

The script produces:

- `normalized.jsonl`: valid normalized records
- `rejected.jsonl`: invalid rows with reasons
- `review.jsonl`: low-confidence rows that may need AI review
- `summary.json`: aggregate stats

## Recommended Python Dependencies

Install these in your own Python environment:

```bash
pip install pandas xlrd rapidfuzz
```

Optional for AI review:

```bash
pip install requests
```

## Example

```bash
python scripts/job_data/preprocess_jobs.py ^
  --input "E:\\Computer\\Job\\career-agent-plus\\a13基于AI的大学生职业规划智能体-JD采样数据（提供时间：2.25） .xls" ^
  --output-dir "E:\\Computer\\Job\\career-agent-plus\\data\\processed\\jobs"
```

## AI Review

AI review is optional and should only be used on low-confidence records.
Do not send all rows to AI.

If later enabled, the script can call an OpenAI-compatible endpoint using:

- `AI_REVIEW_BASE_URL`
- `AI_REVIEW_API_KEY`
- `AI_REVIEW_MODEL`

## Integration

The normalized output should match the Java record:

- `src/main/java/com/zust/qyf/careeragent/domain/dto/importer/NormalizedJobRecord.java`
